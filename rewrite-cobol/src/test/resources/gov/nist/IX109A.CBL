      *HEADER,COBOL,IX109A                                                            
000100 IDENTIFICATION DIVISION.                                         IX1094.2
000200 PROGRAM-ID.                                                      IX1094.2
000300     IX109A.                                                      IX1094.2
000400****************************************************************  IX1094.2
000500*                                                              *  IX1094.2
000600*    VALIDATION FOR:-                                          *  IX1094.2
000700*                                                              *  IX1094.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IX1094.2
000900*                                                              *  IX1094.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".IX1094.2
001100*                                                              *  IX1094.2
001200****************************************************************  IX1094.2
001300*                                                                 IX1094.2
001400*    1.  THE  ROUTINE  CREATES  THE  MASS  STORAGE  FILE  IX-FS3  IX1094.2
001500*        CONTAINING  50  RECORDS. EACH BLOCK CONTAINS 2 RECORDS,  IX1094.2
001600*        EACH  RECORD  CONTAINS  240 CHARACTERS, ORGANIZATION IS  IX1094.2
001700*        INDEXED,  ACCESS  IS SEQUENTIAL. THIS ROUTINE CHECKS AS  IX1094.2
001800*        MANY  FILE STATUS CODES AS POSSIBLE. HOWEVER, IT IS NOT  IX1094.2
001900*        POSSIBLE TO CHECK ALL CODES NEITHER IN THAT PROGRAM NOR  IX1094.2
002000*        IN THE FOLLOWING ONE.                                    IX1094.2
002100*                                                                 IX1094.2
002200*    2. THE ROUTINE READS THE CREATED FILE,VERIFIES IT AND       *IX1094.2
002300*       CHECKS THE FILE STATUS CODES:                             IX1094.2
002400*           00  -  AFTER OPEN OUTPUT                              IX1094.2
002500*           00  -  AFTER WRITE                                    IX1094.2
002600*           21  -  AFTER WRITE  (VIOLATE ASCENDING SEQUENCE)      IX1094.2
002700*           00  -  AFTER CLOSE OUTPUT                             IX1094.2
002800*           00  -  AFTER OPEN INPUT                               IX1094.2
002900*           00  -  AFTER READ    (WITHOUT AT END)                 IX1094.2
003000*           10  -  AFTER READ    (SEE  IX-4, 1.3.4, (2) A)        IX1094.2
003100*           00  -  AFTER CLOSE INPUT                              IX1094.2
003200*           00  -  AFTER OPEN INPUT                               IX1094.2
003300*           00  -  AFTER READ ... END  ...                        IX1094.2
003400*           10  -  AFTER READ ... END  ...                        IX1094.2
003500*           46  -  AFTER READ ... END  ...                        IX1094.2
003600*                                                                 IX1094.2
003700*    4. X-CARDS USED IN THIS PROGRAM:                             IX1094.2
003800*                                                                 IX1094.2
003900*                 XXXXX024                                        IX1094.2
004000*                 XXXXX055.                                       IX1094.2
004100*         P       XXXXX062.                                       IX1094.2
004200*                 XXXXX082.                                       IX1094.2
004300*                 XXXXX083.                                       IX1094.2
004400*         C       XXXXX084                                        IX1094.2
004500*                                                                 IX1094.2
004600*                                                                 IX1094.2
004700 ENVIRONMENT DIVISION.                                            IX1094.2
004800 CONFIGURATION SECTION.                                           IX1094.2
004900 SOURCE-COMPUTER.                                                 IX1094.2
005000     XXXXX082.                                                    IX1094.2
005100 OBJECT-COMPUTER.                                                 IX1094.2
005200     XXXXX083.                                                    IX1094.2
005300 INPUT-OUTPUT SECTION.                                            IX1094.2
005400 FILE-CONTROL.                                                    IX1094.2
005500*    SELECT RAW-DATA   ASSIGN TO                                  IX1094.2
005600*    XXXXX062                                                     IX1094.2
005700*           ORGANIZATION IS INDEXED                               IX1094.2
005800*           ACCESS MODE IS RANDOM                                 IX1094.2
005900*           RECORD KEY IS RAW-DATA-KEY.                           IX1094.2
006000*                                                                 IX1094.2
006100     SELECT PRINT-FILE ASSIGN TO                                  IX1094.2
006200     XXXXX055.                                                    IX1094.2
006300*                                                                 IX1094.2
006400     SELECT IX-FS3 ASSIGN                                         IX1094.2
006500     XXXXX024                                                     IX1094.2
006600     ORGANIZATION IS INDEXED                                      IX1094.2
006700     ACCESS MODE IS SEQUENTIAL                                    IX1094.2
006800     RECORD KEY IS IX-FS3-KEY                                     IX1094.2
006900     FILE STATUS IS IX-FS3-STATUS.                                IX1094.2
007000                                                                  IX1094.2
007100 DATA DIVISION.                                                   IX1094.2
007200                                                                  IX1094.2
007300 FILE SECTION.                                                    IX1094.2
007400*                                                                 IX1094.2
007500*FD  RAW-DATA.                                                    IX1094.2
007600*                                                                 IX1094.2
007700*01  RAW-DATA-SATZ.                                               IX1094.2
007800*    05  RAW-DATA-KEY        PIC X(6).                            IX1094.2
007900*    05  C-DATE              PIC 9(6).                            IX1094.2
008000*    05  C-TIME              PIC 9(8).                            IX1094.2
008100*    05  C-NO-OF-TESTS       PIC 99.                              IX1094.2
008200*    05  C-OK                PIC 999.                             IX1094.2
008300*    05  C-ALL               PIC 999.                             IX1094.2
008400*    05  C-FAIL              PIC 999.                             IX1094.2
008500*    05  C-DELETED           PIC 999.                             IX1094.2
008600*    05  C-INSPECT           PIC 999.                             IX1094.2
008700*    05  C-NOTE              PIC X(13).                           IX1094.2
008800*    05  C-INDENT            PIC X.                               IX1094.2
008900*    05  C-ABORT             PIC X(8).                            IX1094.2
009000                                                                  IX1094.2
009100 FD  PRINT-FILE.                                                  IX1094.2
009200                                                                  IX1094.2
009300 01  PRINT-REC               PIC X(120).                          IX1094.2
009400                                                                  IX1094.2
009500 01  DUMMY-RECORD            PIC X(120).                          IX1094.2
009600                                                                  IX1094.2
009700 FD  IX-FS3                                                       IX1094.2
009800C       DATA RECORDS IX-FS3R1-F-G-240                             IX1094.2
009900C       LABEL RECORD STANDARD                                     IX1094.2
010000        RECORD 240                                                IX1094.2
010100        BLOCK CONTAINS 2 RECORDS.                                 IX1094.2
010200                                                                  IX1094.2
010300 01  IX-FS3R1-F-G-240.                                            IX1094.2
010400     05  IX-FS3-REC-120      PIC X(120).                          IX1094.2
010500     05  IX-FS3-REC-120-240.                                      IX1094.2
010600         10  FILLER          PIC X(8).                            IX1094.2
010700         10  IX-FS3-KEY      PIC X(29).                           IX1094.2
010800         10  FILLER          PIC X(9).                            IX1094.2
010900         10  IX-FS3-ALTER-KEY  PIC X(29).                         IX1094.2
011000         10  FILLER            PIC X(45).                         IX1094.2
011100                                                                  IX1094.2
011200                                                                  IX1094.2
011300 WORKING-STORAGE SECTION.                                         IX1094.2
011400                                                                  IX1094.2
011500 01  GRP-0101.                                                    IX1094.2
011600     05  FILLER              PIC X(10) VALUE "RECORD-KEY".        IX1094.2
011700     05  GRP-0101-KEY        PIC 9(9)  VALUE ZERO.                IX1094.2
011800     05  FILLER              PIC X(10) VALUE "END-OF-KEY".        IX1094.2
011900                                                                  IX1094.2
012000 01  GRP-0102.                                                    IX1094.2
012100     05  FILLER              PIC X(10) VALUE "ALTERN-KEY".        IX1094.2
012200     05  GRP-0102-KEY        PIC 9(9)  VALUE ZERO.                IX1094.2
012300     05  FILLER              PIC X(10) VALUE "END-AL-KEY".        IX1094.2
012400                                                                  IX1094.2
012500 01  WRK-CS-09V00            PIC S9(9) COMP VALUE ZERO.           IX1094.2
012600                                                                  IX1094.2
012700 01  EOF-FLAG                PIC 9 VALUE ZERO.                    IX1094.2
012800                                                                  IX1094.2
012900 01  RECORDS-IN-ERROR        PIC S9(5) COMP VALUE ZERO.           IX1094.2
013000                                                                  IX1094.2
013100 01  ERROR-FLAG              PIC 9 VALUE ZERO.                    IX1094.2
013200                                                                  IX1094.2
013300 01  PERM-ERRORS             PIC S9(5) COMP VALUE ZERO.           IX1094.2
013400                                                                  IX1094.2
013500 01  STATUS-TEST-00          PIC 9 VALUE ZERO.                    IX1094.2
013600                                                                  IX1094.2
013700 01  STATUS-TEST-10          PIC 9 VALUE ZERO.                    IX1094.2
013800 01  STATUS-TEST-READ        PIC 9 VALUE ZERO.                    IX1094.2
013900                                                                  IX1094.2
014000 01  IX-FS3-STATUS.                                               IX1094.2
014100     05  IX-FS3-STAT1        PIC X.                               IX1094.2
014200     05  IX-FS3-STAT2        PIC X.                               IX1094.2
014300                                                                  IX1094.2
014400 01  COUNT-OF-RECS           PIC 9(5).                            IX1094.2
014500                                                                  IX1094.2
014600 01  COUNT-OF-RECORDS REDEFINES COUNT-OF-RECS  PIC 9(5).          IX1094.2
014700                                                                  IX1094.2
014800 01  FILE-RECORD-INFORMATION-REC.                                 IX1094.2
014900     05  FILE-RECORD-INFO-SKELETON.                               IX1094.2
015000         10  FILLER          PIC X(48) VALUE                      IX1094.2
015100              "FILE=      ,RECORD=      /0,RECNO=000000,UPDT=00". IX1094.2
015200         10  FILLER          PIC X(46) VALUE                      IX1094.2
015300                ",ODO=0000,PGM=     ,LRECL=000000,BLKSIZ  =0000". IX1094.2
015400         10  FILLER          PIC X(26) VALUE                      IX1094.2
015500                                    ",LFIL=000000,ORG=  ,LBLR= ". IX1094.2
015600         10  FILLER          PIC X(37) VALUE                      IX1094.2
015700                         ",RECKEY=                             ". IX1094.2
015800         10  FILLER          PIC X(38) VALUE                      IX1094.2
015900                        ",ALTKEY1=                             ". IX1094.2
016000         10  FILLER          PIC X(38) VALUE                      IX1094.2
016100                        ",ALTKEY2=                             ". IX1094.2
016200         10  FILLER          PIC X(7) VALUE SPACE.                IX1094.2
016300     05  FILE-RECORD-INFO             OCCURS 10.                  IX1094.2
016400         10  FILE-RECORD-INFO-P1-120.                             IX1094.2
016500             15  FILLER      PIC X(5).                            IX1094.2
016600             15  XFILE-NAME  PIC X(6).                            IX1094.2
016700             15  FILLER      PIC X(8).                            IX1094.2
016800             15  XRECORD-NAME  PIC X(6).                          IX1094.2
016900             15  FILLER        PIC X(1).                          IX1094.2
017000             15  REELUNIT-NUMBER  PIC 9(1).                       IX1094.2
017100             15  FILLER           PIC X(7).                       IX1094.2
017200             15  XRECORD-NUMBER   PIC 9(6).                       IX1094.2
017300             15  FILLER           PIC X(6).                       IX1094.2
017400             15  UPDATE-NUMBER    PIC 9(2).                       IX1094.2
017500             15  FILLER           PIC X(5).                       IX1094.2
017600             15  ODO-NUMBER       PIC 9(4).                       IX1094.2
017700             15  FILLER           PIC X(5).                       IX1094.2
017800             15  XPROGRAM-NAME    PIC X(5).                       IX1094.2
017900             15  FILLER           PIC X(7).                       IX1094.2
018000             15  XRECORD-LENGTH   PIC 9(6).                       IX1094.2
018100             15  FILLER           PIC X(7).                       IX1094.2
018200             15  CHARS-OR-RECORDS  PIC X(2).                      IX1094.2
018300             15  FILLER            PIC X(1).                      IX1094.2
018400             15  XBLOCK-SIZE       PIC 9(4).                      IX1094.2
018500             15  FILLER            PIC X(6).                      IX1094.2
018600             15  RECORDS-IN-FILE   PIC 9(6).                      IX1094.2
018700             15  FILLER            PIC X(5).                      IX1094.2
018800             15  XFILE-ORGANIZATION  PIC X(2).                    IX1094.2
018900             15  FILLER              PIC X(6).                    IX1094.2
019000             15  XLABEL-TYPE         PIC X(1).                    IX1094.2
019100         10  FILE-RECORD-INFO-P121-240.                           IX1094.2
019200             15  FILLER              PIC X(8).                    IX1094.2
019300             15  XRECORD-KEY         PIC X(29).                   IX1094.2
019400             15  FILLER              PIC X(9).                    IX1094.2
019500             15  ALTERNATE-KEY1      PIC X(29).                   IX1094.2
019600             15  FILLER              PIC X(9).                    IX1094.2
019700             15  ALTERNATE-KEY2      PIC X(29).                   IX1094.2
019800             15  FILLER              PIC X(7).                    IX1094.2
019900                                                                  IX1094.2
020000 01  TEST-RESULTS.                                                IX1094.2
020100     02 FILLER                   PIC X      VALUE SPACE.          IX1094.2
020200     02 FEATURE                  PIC X(20)  VALUE SPACE.          IX1094.2
020300     02 FILLER                   PIC X      VALUE SPACE.          IX1094.2
020400     02 P-OR-F                   PIC X(5)   VALUE SPACE.          IX1094.2
020500     02 FILLER                   PIC X      VALUE SPACE.          IX1094.2
020600     02  PAR-NAME.                                                IX1094.2
020700       03 FILLER                 PIC X(19)  VALUE SPACE.          IX1094.2
020800       03  PARDOT-X              PIC X      VALUE SPACE.          IX1094.2
020900       03 DOTVALUE               PIC 99     VALUE ZERO.           IX1094.2
021000     02 FILLER                   PIC X(8)   VALUE SPACE.          IX1094.2
021100     02 RE-MARK                  PIC X(61).                       IX1094.2
021200 01  TEST-COMPUTED.                                               IX1094.2
021300     02 FILLER                   PIC X(30)  VALUE SPACE.          IX1094.2
021400     02 FILLER                   PIC X(17)  VALUE                 IX1094.2
021500            "       COMPUTED=".                                   IX1094.2
021600     02 COMPUTED-X.                                               IX1094.2
021700     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          IX1094.2
021800     03 COMPUTED-N               REDEFINES COMPUTED-A             IX1094.2
021900                                 PIC -9(9).9(9).                  IX1094.2
022000     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         IX1094.2
022100     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     IX1094.2
022200     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     IX1094.2
022300     03       CM-18V0 REDEFINES COMPUTED-A.                       IX1094.2
022400         04 COMPUTED-18V0                    PIC -9(18).          IX1094.2
022500         04 FILLER                           PIC X.               IX1094.2
022600     03 FILLER PIC X(50) VALUE SPACE.                             IX1094.2
022700 01  TEST-CORRECT.                                                IX1094.2
022800     02 FILLER PIC X(30) VALUE SPACE.                             IX1094.2
022900     02 FILLER PIC X(17) VALUE "       CORRECT =".                IX1094.2
023000     02 CORRECT-X.                                                IX1094.2
023100     03 CORRECT-A                  PIC X(20) VALUE SPACE.         IX1094.2
023200     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      IX1094.2
023300     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         IX1094.2
023400     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     IX1094.2
023500     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     IX1094.2
023600     03      CR-18V0 REDEFINES CORRECT-A.                         IX1094.2
023700         04 CORRECT-18V0                     PIC -9(18).          IX1094.2
023800         04 FILLER                           PIC X.               IX1094.2
023900     03 FILLER PIC X(2) VALUE SPACE.                              IX1094.2
024000     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     IX1094.2
024100 01  CCVS-C-1.                                                    IX1094.2
024200     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PAIX1094.2
024300-    "SS  PARAGRAPH-NAME                                          IX1094.2
024400-    "       REMARKS".                                            IX1094.2
024500     02 FILLER                     PIC X(20)    VALUE SPACE.      IX1094.2
024600 01  CCVS-C-2.                                                    IX1094.2
024700     02 FILLER                     PIC X        VALUE SPACE.      IX1094.2
024800     02 FILLER                     PIC X(6)     VALUE "TESTED".   IX1094.2
024900     02 FILLER                     PIC X(15)    VALUE SPACE.      IX1094.2
025000     02 FILLER                     PIC X(4)     VALUE "FAIL".     IX1094.2
025100     02 FILLER                     PIC X(94)    VALUE SPACE.      IX1094.2
025200 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       IX1094.2
025300 01  REC-CT                        PIC 99       VALUE ZERO.       IX1094.2
025400 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       IX1094.2
025500 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       IX1094.2
025600 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       IX1094.2
025700 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       IX1094.2
025800 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       IX1094.2
025900 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       IX1094.2
026000 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      IX1094.2
026100 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       IX1094.2
026200 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     IX1094.2
026300 01  CCVS-H-1.                                                    IX1094.2
026400     02  FILLER                    PIC X(39)    VALUE SPACES.     IX1094.2
026500     02  FILLER                    PIC X(42)    VALUE             IX1094.2
026600     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 IX1094.2
026700     02  FILLER                    PIC X(39)    VALUE SPACES.     IX1094.2
026800 01  CCVS-H-2A.                                                   IX1094.2
026900   02  FILLER                        PIC X(40)  VALUE SPACE.      IX1094.2
027000   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  IX1094.2
027100   02  FILLER                        PIC XXXX   VALUE             IX1094.2
027200     "4.2 ".                                                      IX1094.2
027300   02  FILLER                        PIC X(28)  VALUE             IX1094.2
027400            " COPY - NOT FOR DISTRIBUTION".                       IX1094.2
027500   02  FILLER                        PIC X(41)  VALUE SPACE.      IX1094.2
027600                                                                  IX1094.2
027700 01  CCVS-H-2B.                                                   IX1094.2
027800   02  FILLER                        PIC X(15)  VALUE             IX1094.2
027900            "TEST RESULT OF ".                                    IX1094.2
028000   02  TEST-ID                       PIC X(9).                    IX1094.2
028100   02  FILLER                        PIC X(4)   VALUE             IX1094.2
028200            " IN ".                                               IX1094.2
028300   02  FILLER                        PIC X(12)  VALUE             IX1094.2
028400     " HIGH       ".                                              IX1094.2
028500   02  FILLER                        PIC X(22)  VALUE             IX1094.2
028600            " LEVEL VALIDATION FOR ".                             IX1094.2
028700   02  FILLER                        PIC X(58)  VALUE             IX1094.2
028800     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IX1094.2
028900 01  CCVS-H-3.                                                    IX1094.2
029000     02  FILLER                      PIC X(34)  VALUE             IX1094.2
029100            " FOR OFFICIAL USE ONLY    ".                         IX1094.2
029200     02  FILLER                      PIC X(58)  VALUE             IX1094.2
029300     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".IX1094.2
029400     02  FILLER                      PIC X(28)  VALUE             IX1094.2
029500            "  COPYRIGHT   1985 ".                                IX1094.2
029600 01  CCVS-E-1.                                                    IX1094.2
029700     02 FILLER                       PIC X(52)  VALUE SPACE.      IX1094.2
029800     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              IX1094.2
029900     02 ID-AGAIN                     PIC X(9).                    IX1094.2
030000     02 FILLER                       PIC X(45)  VALUE SPACES.     IX1094.2
030100 01  CCVS-E-2.                                                    IX1094.2
030200     02  FILLER                      PIC X(31)  VALUE SPACE.      IX1094.2
030300     02  FILLER                      PIC X(21)  VALUE SPACE.      IX1094.2
030400     02 CCVS-E-2-2.                                               IX1094.2
030500         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      IX1094.2
030600         03 FILLER                   PIC X      VALUE SPACE.      IX1094.2
030700         03 ENDER-DESC               PIC X(44)  VALUE             IX1094.2
030800            "ERRORS ENCOUNTERED".                                 IX1094.2
030900 01  CCVS-E-3.                                                    IX1094.2
031000     02  FILLER                      PIC X(22)  VALUE             IX1094.2
031100            " FOR OFFICIAL USE ONLY".                             IX1094.2
031200     02  FILLER                      PIC X(12)  VALUE SPACE.      IX1094.2
031300     02  FILLER                      PIC X(58)  VALUE             IX1094.2
031400     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IX1094.2
031500     02  FILLER                      PIC X(13)  VALUE SPACE.      IX1094.2
031600     02 FILLER                       PIC X(15)  VALUE             IX1094.2
031700             " COPYRIGHT 1985".                                   IX1094.2
031800 01  CCVS-E-4.                                                    IX1094.2
031900     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      IX1094.2
032000     02 FILLER                       PIC X(4)   VALUE " OF ".     IX1094.2
032100     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      IX1094.2
032200     02 FILLER                       PIC X(40)  VALUE             IX1094.2
032300      "  TESTS WERE EXECUTED SUCCESSFULLY".                       IX1094.2
032400 01  XXINFO.                                                      IX1094.2
032500     02 FILLER                       PIC X(19)  VALUE             IX1094.2
032600            "*** INFORMATION ***".                                IX1094.2
032700     02 INFO-TEXT.                                                IX1094.2
032800       04 FILLER                     PIC X(8)   VALUE SPACE.      IX1094.2
032900       04 XXCOMPUTED                 PIC X(20).                   IX1094.2
033000       04 FILLER                     PIC X(5)   VALUE SPACE.      IX1094.2
033100       04 XXCORRECT                  PIC X(20).                   IX1094.2
033200     02 INF-ANSI-REFERENCE           PIC X(48).                   IX1094.2
033300 01  HYPHEN-LINE.                                                 IX1094.2
033400     02 FILLER  PIC IS X VALUE IS SPACE.                          IX1094.2
033500     02 FILLER  PIC IS X(65)    VALUE IS "************************IX1094.2
033600-    "*****************************************".                 IX1094.2
033700     02 FILLER  PIC IS X(54)    VALUE IS "************************IX1094.2
033800-    "******************************".                            IX1094.2
033900 01  TEST-NO                         PIC 99.                      IX1094.2
034000 01  CCVS-PGM-ID                     PIC X(9)   VALUE             IX1094.2
034100     "IX109A".                                                    IX1094.2
034200 PROCEDURE DIVISION.                                              IX1094.2
034300 DECLARATIVES.                                                    IX1094.2
034400                                                                  IX1094.2
034500 SECT-IX109-0002 SECTION.                                         IX1094.2
034600     USE AFTER EXCEPTION PROCEDURE ON IX-FS3.                     IX1094.2
034700 INPUT-PROCESS.                                                   IX1094.2
034800     IF TEST-NO = 13                                              IX1094.2
034900        GO TO D-R-TEST-F1-06-1.                                   IX1094.2
035000     IF STATUS-TEST-10 EQUAL TO 1                                 IX1094.2
035100        IF  IX-FS3-STAT1 EQUAL TO "1"                             IX1094.2
035200            MOVE 1 TO EOF-FLAG                                    IX1094.2
035300        ELSE                                                      IX1094.2
035400           IF  IX-FS3-STAT1 GREATER THAN "1"                      IX1094.2
035500           MOVE 1 TO PERM-ERRORS.                                 IX1094.2
035600     GO TO DECL-EXIT.                                             IX1094.2
035700 D-R-TEST-F1-06-1.                                                IX1094.2
035800     IF IX-FS3-STATUS EQUAL TO "46"                               IX1094.2
035900         GO TO D-R-PASS-F1-06-0.                                  IX1094.2
036000 D-R-FAIL-F1-06-0.                                                IX1094.2
036100     MOVE IX-FS3-STATUS TO COMPUTED-A.                            IX1094.2
036200     MOVE "46" TO CORRECT-X.                                      IX1094.2
036300     MOVE "IX-5, 1.3.4, (5) E 3)" TO RE-MARK.                     IX1094.2
036400     PERFORM D-FAIL.                                              IX1094.2
036500     GO TO D-R-WRITE-F1-06-0.                                     IX1094.2
036600 D-R-PASS-F1-06-0.                                                IX1094.2
036700     PERFORM D-PASS.                                              IX1094.2
036800 D-R-WRITE-F1-06-0.                                               IX1094.2
036900     MOVE "READ.        46 EXP." TO FEATURE.                      IX1094.2
037000     MOVE "REA-TEST-F1-06-0" TO PAR-NAME.                         IX1094.2
037100     PERFORM D-PRINT-DETAIL.                                      IX1094.2
037200 D-CLOSE-FILES.                                                   IX1094.2
037300     CLOSE IX-FS3.                                                IX1094.2
037400*    OPEN I-O RAW-DATA.                                           IX1094.2
037500*    MOVE CCVS-PGM-ID TO RAW-DATA-KEY.                            IX1094.2
037600*    READ RAW-DATA INVALID KEY GO TO D-END-E-2.                   IX1094.2
037700*    MOVE "OK.     " TO C-ABORT.                                  IX1094.2
037800*    MOVE PASS-COUNTER TO C-OK.                                   IX1094.2
037900*    MOVE ERROR-HOLD   TO C-ALL.                                  IX1094.2
038000*    MOVE ERROR-COUNTER TO C-FAIL.                                IX1094.2
038100*    MOVE DELETE-COUNTER TO C-DELETED.                            IX1094.2
038200*    MOVE INSPECT-COUNTER TO C-INSPECT.                           IX1094.2
038300*    REWRITE RAW-DATA-SATZ INVALID KEY GO TO D-END-E-2.           IX1094.2
038400*D-END-E-2.                                                       IX1094.2
038500*    CLOSE RAW-DATA.                                              IX1094.2
038600     PERFORM D-END-ROUTINE THRU D-END-ROUTINE-13.                 IX1094.2
038700     CLOSE PRINT-FILE.                                            IX1094.2
038800 D-TERMINATE-CCVS.                                                IX1094.2
038900*    EXIT PROGRAM.                                                IX1094.2
039000*D-TERMINATE-CALL.                                                IX1094.2
039100     STOP     RUN.                                                IX1094.2
039200 D-PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.         IX1094.2
039300 D-FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.        IX1094.2
039400 D-PRINT-DETAIL.                                                  IX1094.2
039500     IF   REC-CT NOT EQUAL TO ZERO                                IX1094.2
039600          MOVE "." TO PARDOT-X                                    IX1094.2
039700          MOVE REC-CT TO DOTVALUE.                                IX1094.2
039800     MOVE TEST-RESULTS TO PRINT-REC.                              IX1094.2
039900     PERFORM D-WRITE-LINE.                                        IX1094.2
040000     IF   P-OR-F EQUAL TO "FAIL*"                                 IX1094.2
040100          PERFORM D-WRITE-LINE                                    IX1094.2
040200          PERFORM D-FAIL-ROUTINE THRU D-FAIL-ROUTINE-EX           IX1094.2
040300     ELSE                                                         IX1094.2
040400          PERFORM D-BAIL-OUT THRU D-BAIL-OUT-EX.                  IX1094.2
040500     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              IX1094.2
040600     MOVE SPACE TO CORRECT-X.                                     IX1094.2
040700     IF   REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.           IX1094.2
040800     MOVE SPACE TO RE-MARK.                                       IX1094.2
040900 D-END-ROUTINE.                                                   IX1094.2
041000     MOVE HYPHEN-LINE TO DUMMY-RECORD.                            IX1094.2
041100     PERFORM D-WRITE-LINE 5 TIMES.                                IX1094.2
041200 D-END-RTN-EXIT.                                                  IX1094.2
041300     MOVE CCVS-E-1 TO DUMMY-RECORD.                               IX1094.2
041400     PERFORM D-WRITE-LINE 2 TIMES.                                IX1094.2
041500 D-END-ROUTINE-1.                                                 IX1094.2
041600     ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO       IX1094.2
041700     ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.                IX1094.2
041800     ADD PASS-COUNTER TO ERROR-HOLD.                              IX1094.2
041900     MOVE PASS-COUNTER TO CCVS-E-4-1.                             IX1094.2
042000     MOVE ERROR-HOLD TO CCVS-E-4-2.                               IX1094.2
042100     MOVE CCVS-E-4 TO CCVS-E-2-2.                                 IX1094.2
042200     MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM D-WRITE-LINE.          IX1094.2
042300  D-END-ROUTINE-12.                                               IX1094.2
042400     MOVE "TEST(S) FAILED" TO ENDER-DESC.                         IX1094.2
042500     IF  ERROR-COUNTER IS EQUAL TO ZERO                           IX1094.2
042600         MOVE "NO " TO ERROR-TOTAL                                IX1094.2
042700     ELSE                                                         IX1094.2
042800         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       IX1094.2
042900     MOVE    CCVS-E-2 TO DUMMY-RECORD.                            IX1094.2
043000     PERFORM D-WRITE-LINE.                                        IX1094.2
043100 D-END-ROUTINE-13.                                                IX1094.2
043200     IF  DELETE-COUNTER IS EQUAL TO ZERO                          IX1094.2
043300         MOVE "NO " TO ERROR-TOTAL  ELSE                          IX1094.2
043400         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      IX1094.2
043500     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   IX1094.2
043600     MOVE CCVS-E-2 TO DUMMY-RECORD.                               IX1094.2
043700     PERFORM D-WRITE-LINE.                                        IX1094.2
043800     IF   INSPECT-COUNTER EQUAL TO ZERO                           IX1094.2
043900          MOVE "NO " TO ERROR-TOTAL                               IX1094.2
044000     ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                    IX1094.2
044100     MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.             IX1094.2
044200     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM D-WRITE-LINE.         IX1094.2
044300     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM D-WRITE-LINE.         IX1094.2
044400 D-WRITE-LINE.                                                    IX1094.2
044500     ADD 1 TO RECORD-COUNT.                                       IX1094.2
044600*    IF RECORD-COUNT GREATER 42                                   IX1094.2
044700*       MOVE DUMMY-RECORD TO DUMMY-HOLD                           IX1094.2
044800*       MOVE SPACE TO DUMMY-RECORD                                IX1094.2
044900*       WRITE DUMMY-RECORD AFTER ADVANCING PAGE                   IX1094.2
045000*       MOVE CCVS-H-1  TO DUMMY-RECORD PERFORM D-WRT-LN 2 TIMES   IX1094.2
045100*       MOVE CCVS-H-2A TO DUMMY-RECORD PERFORM D-WRT-LN 2 TIMES   IX1094.2
045200*       MOVE CCVS-H-2B TO DUMMY-RECORD PERFORM D-WRT-LN 3 TIMES   IX1094.2
045300*       MOVE CCVS-H-3  TO DUMMY-RECORD PERFORM D-WRT-LN 3 TIMES   IX1094.2
045400*       MOVE CCVS-C-1  TO DUMMY-RECORD PERFORM D-WRT-LN           IX1094.2
045500*       MOVE CCVS-C-2  TO DUMMY-RECORD PERFORM D-WRT-LN           IX1094.2
045600*       MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM D-WRT-LN         IX1094.2
045700*       MOVE DUMMY-HOLD TO DUMMY-RECORD                           IX1094.2
045800*       MOVE ZERO TO RECORD-COUNT.                                IX1094.2
045900     PERFORM D-WRT-LN.                                            IX1094.2
046000 D-WRT-LN.                                                        IX1094.2
046100     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               IX1094.2
046200     MOVE SPACE TO DUMMY-RECORD.                                  IX1094.2
046300 D-FAIL-ROUTINE.                                                  IX1094.2
046400     IF   COMPUTED-X NOT EQUAL TO SPACE                           IX1094.2
046500          GO TO D-FAIL-ROUTINE-WRITE.                             IX1094.2
046600     IF   CORRECT-X NOT EQUAL TO SPACE GO TO D-FAIL-ROUTINE-WRITE.IX1094.2
046700     MOVE ANSI-REFERENCE TO INF-ANSI-REFERENCE.                   IX1094.2
046800     MOVE "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.    IX1094.2
046900     MOVE XXINFO TO DUMMY-RECORD. PERFORM D-WRITE-LINE 2 TIMES.   IX1094.2
047000     MOVE SPACES TO INF-ANSI-REFERENCE.                           IX1094.2
047100     GO TO D-FAIL-ROUTINE-EX.                                     IX1094.2
047200 D-FAIL-ROUTINE-WRITE.                                            IX1094.2
047300     MOVE TEST-COMPUTED TO PRINT-REC PERFORM D-WRITE-LINE         IX1094.2
047400     MOVE ANSI-REFERENCE TO COR-ANSI-REFERENCE.                   IX1094.2
047500     MOVE TEST-CORRECT TO PRINT-REC PERFORM D-WRITE-LINE 2 TIMES. IX1094.2
047600     MOVE SPACES TO COR-ANSI-REFERENCE.                           IX1094.2
047700 D-FAIL-ROUTINE-EX. EXIT.                                         IX1094.2
047800 D-BAIL-OUT.                                                      IX1094.2
047900     IF  COMPUTED-A NOT EQUAL TO SPACE GO TO D-BAIL-OUT-WRITE.    IX1094.2
048000     IF  CORRECT-A EQUAL TO SPACE GO TO D-BAIL-OUT-EX.            IX1094.2
048100 D-BAIL-OUT-WRITE.                                                IX1094.2
048200     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  IX1094.2
048300     MOVE ANSI-REFERENCE TO INF-ANSI-REFERENCE.                   IX1094.2
048400     MOVE XXINFO TO DUMMY-RECORD. PERFORM D-WRITE-LINE 2 TIMES.   IX1094.2
048500     MOVE SPACES TO INF-ANSI-REFERENCE.                           IX1094.2
048600 D-BAIL-OUT-EX. EXIT.                                             IX1094.2
048700 DECL-EXIT.  EXIT.                                                IX1094.2
048800 END DECLARATIVES.                                                IX1094.2
048900                                                                  IX1094.2
049000                                                                  IX1094.2
049100 CCVS1 SECTION.                                                   IX1094.2
049200 OPEN-FILES.                                                      IX1094.2
049300*    OPEN I-O RAW-DATA.                                           IX1094.2
049400*    MOVE CCVS-PGM-ID TO RAW-DATA-KEY.                            IX1094.2
049500*    READ RAW-DATA INVALID KEY GO TO END-E-1.                     IX1094.2
049600*    MOVE "ABORTED " TO C-ABORT.                                  IX1094.2
049700*    ADD 1 TO C-NO-OF-TESTS.                                      IX1094.2
049800*    ACCEPT C-DATE  FROM DATE.                                    IX1094.2
049900*    ACCEPT C-TIME  FROM TIME.                                    IX1094.2
050000*    REWRITE RAW-DATA-SATZ INVALID KEY GO TO END-E-1.             IX1094.2
050100*END-E-1.                                                         IX1094.2
050200*    CLOSE RAW-DATA.                                              IX1094.2
050300     OPEN    OUTPUT PRINT-FILE.                                   IX1094.2
050400     MOVE  CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.  IX1094.2
050500     MOVE    SPACE TO TEST-RESULTS.                               IX1094.2
050600     PERFORM HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.              IX1094.2
050700     MOVE    ZERO TO REC-SKL-SUB.                                 IX1094.2
050800     PERFORM CCVS-INIT-FILE 9 TIMES.                              IX1094.2
050900 CCVS-INIT-FILE.                                                  IX1094.2
051000     ADD     1 TO REC-SKL-SUB.                                    IX1094.2
051100     MOVE    FILE-RECORD-INFO-SKELETON                            IX1094.2
051200          TO FILE-RECORD-INFO (REC-SKL-SUB).                      IX1094.2
051300 CCVS-INIT-EXIT.                                                  IX1094.2
051400     GO TO CCVS1-EXIT.                                            IX1094.2
051500 CLOSE-FILES.                                                     IX1094.2
051600*    OPEN I-O RAW-DATA.                                           IX1094.2
051700*    MOVE CCVS-PGM-ID TO RAW-DATA-KEY.                            IX1094.2
051800*    READ RAW-DATA INVALID KEY GO TO END-E-2.                     IX1094.2
051900*    MOVE "OK.     " TO C-ABORT.                                  IX1094.2
052000*    MOVE PASS-COUNTER TO C-OK.                                   IX1094.2
052100*    MOVE ERROR-HOLD   TO C-ALL.                                  IX1094.2
052200*    MOVE ERROR-COUNTER TO C-FAIL.                                IX1094.2
052300*    MOVE DELETE-COUNTER TO C-DELETED.                            IX1094.2
052400*    MOVE INSPECT-COUNTER TO C-INSPECT.                           IX1094.2
052500*    REWRITE RAW-DATA-SATZ INVALID KEY GO TO END-E-2.             IX1094.2
052600*END-E-2.                                                         IX1094.2
052700*    CLOSE RAW-DATA.                                              IX1094.2
052800     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   IX1094.2
052900 TERMINATE-CCVS.                                                  IX1094.2
053000*    EXIT PROGRAM.                                                IX1094.2
053100*TERMINATE-CALL.                                                  IX1094.2
053200     STOP     RUN.                                                IX1094.2
053300 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         IX1094.2
053400 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           IX1094.2
053500 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          IX1094.2
053600 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      IX1094.2
053700     MOVE "****TEST DELETED****" TO RE-MARK.                      IX1094.2
053800 PRINT-DETAIL.                                                    IX1094.2
053900     IF REC-CT NOT EQUAL TO ZERO                                  IX1094.2
054000             MOVE "." TO PARDOT-X                                 IX1094.2
054100             MOVE REC-CT TO DOTVALUE.                             IX1094.2
054200     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      IX1094.2
054300     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               IX1094.2
054400        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 IX1094.2
054500          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 IX1094.2
054600     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              IX1094.2
054700     MOVE SPACE TO CORRECT-X.                                     IX1094.2
054800     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         IX1094.2
054900     MOVE     SPACE TO RE-MARK.                                   IX1094.2
055000 HEAD-ROUTINE.                                                    IX1094.2
055100     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  IX1094.2
055200     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  IX1094.2
055300     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  IX1094.2
055400     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  IX1094.2
055500 COLUMN-NAMES-ROUTINE.                                            IX1094.2
055600     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IX1094.2
055700     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IX1094.2
055800     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        IX1094.2
055900 END-ROUTINE.                                                     IX1094.2
056000     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.IX1094.2
056100 END-RTN-EXIT.                                                    IX1094.2
056200     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IX1094.2
056300 END-ROUTINE-1.                                                   IX1094.2
056400      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      IX1094.2
056500      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               IX1094.2
056600      ADD PASS-COUNTER TO ERROR-HOLD.                             IX1094.2
056700*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   IX1094.2
056800      MOVE PASS-COUNTER TO CCVS-E-4-1.                            IX1094.2
056900      MOVE ERROR-HOLD TO CCVS-E-4-2.                              IX1094.2
057000      MOVE CCVS-E-4 TO CCVS-E-2-2.                                IX1094.2
057100      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           IX1094.2
057200  END-ROUTINE-12.                                                 IX1094.2
057300      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        IX1094.2
057400     IF       ERROR-COUNTER IS EQUAL TO ZERO                      IX1094.2
057500         MOVE "NO " TO ERROR-TOTAL                                IX1094.2
057600         ELSE                                                     IX1094.2
057700         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       IX1094.2
057800     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           IX1094.2
057900     PERFORM WRITE-LINE.                                          IX1094.2
058000 END-ROUTINE-13.                                                  IX1094.2
058100     IF DELETE-COUNTER IS EQUAL TO ZERO                           IX1094.2
058200         MOVE "NO " TO ERROR-TOTAL  ELSE                          IX1094.2
058300         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      IX1094.2
058400     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   IX1094.2
058500     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IX1094.2
058600      IF   INSPECT-COUNTER EQUAL TO ZERO                          IX1094.2
058700          MOVE "NO " TO ERROR-TOTAL                               IX1094.2
058800      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   IX1094.2
058900      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            IX1094.2
059000      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          IX1094.2
059100     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IX1094.2
059200 WRITE-LINE.                                                      IX1094.2
059300     ADD 1 TO RECORD-COUNT.                                       IX1094.2
059400*    IF RECORD-COUNT GREATER 42                                   IX1094.2
059500*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          IX1094.2
059600*        MOVE SPACE TO DUMMY-RECORD                               IX1094.2
059700*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  IX1094.2
059800*        MOVE CCVS-H-1  TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES    IX1094.2
059900*        MOVE CCVS-H-2A TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES    IX1094.2
060000*        MOVE CCVS-H-2B TO DUMMY-RECORD PERFORM WRT-LN 3 TIMES    IX1094.2
060100*        MOVE CCVS-H-3  TO DUMMY-RECORD PERFORM WRT-LN 3 TIMES    IX1094.2
060200*        MOVE CCVS-C-1  TO DUMMY-RECORD PERFORM WRT-LN            IX1094.2
060300*        MOVE CCVS-C-2  TO DUMMY-RECORD PERFORM WRT-LN            IX1094.2
060400*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          IX1094.2
060500*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          IX1094.2
060600*        MOVE ZERO TO RECORD-COUNT.                               IX1094.2
060700     PERFORM WRT-LN.                                              IX1094.2
060800 WRT-LN.                                                          IX1094.2
060900     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               IX1094.2
061000     MOVE SPACE TO DUMMY-RECORD.                                  IX1094.2
061100 BLANK-LINE-PRINT.                                                IX1094.2
061200     PERFORM WRT-LN.                                              IX1094.2
061300 FAIL-ROUTINE.                                                    IX1094.2
061400     IF     COMPUTED-X NOT EQUAL TO SPACE                         IX1094.2
061500            GO TO   FAIL-ROUTINE-WRITE.                           IX1094.2
061600     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.IX1094.2
061700     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 IX1094.2
061800     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   IX1094.2
061900     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IX1094.2
062000     MOVE   SPACES TO INF-ANSI-REFERENCE.                         IX1094.2
062100     GO TO  FAIL-ROUTINE-EX.                                      IX1094.2
062200 FAIL-ROUTINE-WRITE.                                              IX1094.2
062300     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         IX1094.2
062400     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 IX1094.2
062500     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. IX1094.2
062600     MOVE   SPACES TO COR-ANSI-REFERENCE.                         IX1094.2
062700 FAIL-ROUTINE-EX. EXIT.                                           IX1094.2
062800 BAIL-OUT.                                                        IX1094.2
062900     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   IX1094.2
063000     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           IX1094.2
063100 BAIL-OUT-WRITE.                                                  IX1094.2
063200     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  IX1094.2
063300     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 IX1094.2
063400     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IX1094.2
063500     MOVE   SPACES TO INF-ANSI-REFERENCE.                         IX1094.2
063600 BAIL-OUT-EX. EXIT.                                               IX1094.2
063700 CCVS1-EXIT.                                                      IX1094.2
063800     EXIT.                                                        IX1094.2
063900                                                                  IX1094.2
064000 SECT-IX109A-0003 SECTION.                                        IX1094.2
064100 SEQ-INIT-010.                                                    IX1094.2
064200     MOVE ZERO TO TEST-NO.                                        IX1094.2
064300     MOVE "IX-FS3" TO XFILE-NAME (1).                             IX1094.2
064400     MOVE "R1-F-G" TO XRECORD-NAME (1).                           IX1094.2
064500     MOVE CCVS-PGM-ID TO XPROGRAM-NAME (1).                       IX1094.2
064600     MOVE 000240 TO XRECORD-LENGTH (1).                           IX1094.2
064700     MOVE "RC" TO CHARS-OR-RECORDS (1).                           IX1094.2
064800     MOVE 0002 TO XBLOCK-SIZE (1).                                IX1094.2
064900     MOVE 000050 TO RECORDS-IN-FILE (1).                          IX1094.2
065000     MOVE "IX" TO XFILE-ORGANIZATION (1).                         IX1094.2
065100     MOVE "S" TO XLABEL-TYPE (1).                                 IX1094.2
065200     MOVE 000001 TO XRECORD-NUMBER (1).                           IX1094.2
065300     MOVE 0 TO COUNT-OF-RECS.                                     IX1094.2
065400                                                                  IX1094.2
065500******************************************************************IX1094.2
065600*   TEST  1                                                      *IX1094.2
065700*         OPEN OUTPUT ...                 00 EXPECTED            *IX1094.2
065800*         IX-3, 1.3.4 (1) A                                      *IX1094.2
065900*    STATUS 00 CHECK ON OUTPUT FILE IX-FS3                       *IX1094.2
066000*    THE OUTPUT STATEMENT IS SUCCESSFULLY EXECUTED               *IX1094.2
066100******************************************************************IX1094.2
066200 OPN-INIT-GF-01-0.                                                IX1094.2
066300     MOVE 1 TO STATUS-TEST-00.                                    IX1094.2
066400     MOVE SPACES TO IX-FS3-STATUS.                                IX1094.2
066500     MOVE "OPEN OUTPUT: 00 EXP." TO FEATURE.                      IX1094.2
066600     MOVE "OPN-TEST-GF-01-0" TO PAR-NAME.                         IX1094.2
066700     OPEN                                                         IX1094.2
066800        OUTPUT IX-FS3.                                            IX1094.2
066900     IF IX-FS3-STATUS EQUAL TO "00"                               IX1094.2
067000         GO TO OPN-PASS-GF-01-0.                                  IX1094.2
067100 OPN-FAIL-GF-01-0.                                                IX1094.2
067200     MOVE "IX-3, 1.3.4, (1) A. " TO RE-MARK.                      IX1094.2
067300     PERFORM FAIL.                                                IX1094.2
067400     MOVE IX-FS3-STATUS TO COMPUTED-A.                            IX1094.2
067500     MOVE "00" TO CORRECT-X.                                      IX1094.2
067600     GO TO OPN-WRITE-GF-01-0.                                     IX1094.2
067700 OPN-PASS-GF-01-0.                                                IX1094.2
067800     PERFORM PASS.                                                IX1094.2
067900 OPN-WRITE-GF-01-0.                                               IX1094.2
068000     PERFORM PRINT-DETAIL.                                        IX1094.2
068100******************************************************************IX1094.2
068200*   TEST  2                                                      *IX1094.2
068300*         WRITE                           00 EXPECTED            *IX1094.2
068400*         IX-3, 1.3.4 (1) A                                      *IX1094.2
068500*    CREATING A INDEXED FILE WITH 50 RECORDS                     *IX1094.2
068600*    KEY:  FROM  000000001 TO 000000050                          *IX1094.2
068700******************************************************************IX1094.2
068800 WRI-INIT-GF-01-0.                                                IX1094.2
068900     MOVE SPACES TO IX-FS3-STATUS.                                IX1094.2
069000     MOVE 0 TO STATUS-TEST-00.                                    IX1094.2
069100     MOVE "WRITE: 00 EXPECTED" TO FEATURE.                        IX1094.2
069200     MOVE "WRI-TEST-GF-01-0" TO PAR-NAME.                         IX1094.2
069300 WRI-TEST-GF-01-0.                                                IX1094.2
069400     MOVE XRECORD-NUMBER (1) TO GRP-0101-KEY.                     IX1094.2
069500     MOVE GRP-0101 TO XRECORD-KEY (1).                            IX1094.2
069600     MOVE GRP-0102 TO ALTERNATE-KEY1 (1).                         IX1094.2
069700*    THE VALUE OF THE ALTERNATE KEY IS 50 TIMES UNCHANGED        *IX1094.2
069800     MOVE FILE-RECORD-INFO (1) TO IX-FS3R1-F-G-240.               IX1094.2
069900     WRITE IX-FS3R1-F-G-240.                                      IX1094.2
070000     IF IX-FS3-STATUS  NOT = "00"                                 IX1094.2
070100         ADD 1 TO RECORDS-IN-ERROR                                IX1094.2
070200     ELSE                                                         IX1094.2
070300         ADD 1 TO COUNT-OF-RECS.                                  IX1094.2
070400     IF XRECORD-NUMBER (1) EQUAL TO 50                            IX1094.2
070500         GO TO WRI-TEST-GF-01-1.                                  IX1094.2
070600     ADD 1 TO XRECORD-NUMBER (1).                                 IX1094.2
070700     GO TO WRI-TEST-GF-01-0.                                      IX1094.2
070800 WRI-TEST-GF-01-1.                                                IX1094.2
070900     IF RECORDS-IN-ERROR EQUAL TO ZERO                            IX1094.2
071000         GO TO WRI-PASS-GF-01-0.                                  IX1094.2
071100 WRI-FAIL-GF-01-0.                                                IX1094.2
071200     MOVE "IX-3, 1.3.4, (1) A. " TO RE-MARK.                      IX1094.2
071300     PERFORM FAIL.                                                IX1094.2
071400     MOVE "RECORDS IN ERROR =" TO COMPUTED-A.                     IX1094.2
071500     MOVE RECORDS-IN-ERROR TO CORRECT-18V0.                       IX1094.2
071600     GO TO WRI-WRITE-GF-01-0.                                     IX1094.2
071700 WRI-PASS-GF-01-0.                                                IX1094.2
071800     PERFORM PASS.                                                IX1094.2
071900 WRI-WRITE-GF-01-0.                                               IX1094.2
072000     PERFORM PRINT-DETAIL.                                        IX1094.2
072100     MOVE "FILE CREATED, RECS =" TO COMPUTED-A.                   IX1094.2
072200     MOVE "CREATE FILE IX-FS3" TO FEATURE.                        IX1094.2
072300     MOVE "WRI-TEST-GF-01-1" TO PAR-NAME.                         IX1094.2
072400     MOVE COUNT-OF-RECS TO CORRECT-18V0.                          IX1094.2
072500     PERFORM PRINT-DETAIL.                                        IX1094.2
072600                                                                  IX1094.2
072700******************************************************************IX1094.2
072800*   TEST  3                                                      *IX1094.2
072900*         WRITE  (WRONG SEQUENCE)         21 EXPECTED            *IX1094.2
073000*         IX-4, 1.3.4 (3) A                                      *IX1094.2
073100*    KEY:        000000049                                       *IX1094.2
073200******************************************************************IX1094.2
073300 WRI-INIT-GF-02-0.                                                IX1094.2
073400     MOVE SPACES TO IX-FS3-STATUS.                                IX1094.2
073500     MOVE 0 TO STATUS-TEST-00.                                    IX1094.2
073600     MOVE "WRITE:       21 EXP." TO FEATURE.                      IX1094.2
073700     MOVE "WRI-TEST-GF-02-0" TO PAR-NAME.                         IX1094.2
073800     MOVE 49 TO XRECORD-NUMBER (1).                               IX1094.2
073900 WRI-TEST-GF-02-0.                                                IX1094.2
074000     MOVE XRECORD-NUMBER (1) TO GRP-0101-KEY.                     IX1094.2
074100     MOVE GRP-0101 TO XRECORD-KEY (1).                            IX1094.2
074200     MOVE GRP-0102 TO ALTERNATE-KEY1 (1).                         IX1094.2
074300     MOVE FILE-RECORD-INFO (1) TO IX-FS3R1-F-G-240.               IX1094.2
074400     WRITE IX-FS3R1-F-G-240 INVALID KEY GO TO WRI-TEST-GF-02-1.   IX1094.2
074500 WRI-TEST-GF-02-1.                                                IX1094.2
074600     IF IX-FS3-STATUS  = "21"                                     IX1094.2
074700         GO TO WRI-PASS-GF-02-0.                                  IX1094.2
074800 WRI-FAIL-GF-02-0.                                                IX1094.2
074900     MOVE "IX-4, 1.3.4, (3) A. " TO RE-MARK.                      IX1094.2
075000     PERFORM FAIL.                                                IX1094.2
075100     MOVE  IX-FS3-STATUS      TO COMPUTED-A.                      IX1094.2
075200     MOVE "21" TO CORRECT-X.                                      IX1094.2
075300     GO TO WRI-WRITE-GF-02-0.                                     IX1094.2
075400 WRI-PASS-GF-02-0.                                                IX1094.2
075500     PERFORM PASS.                                                IX1094.2
075600 WRI-WRITE-GF-02-0.                                               IX1094.2
075700     PERFORM PRINT-DETAIL.                                        IX1094.2
075800                                                                  IX1094.2
075900******************************************************************IX1094.2
076000*   TEST  4                                                      *IX1094.2
076100*         CLOSE OUTPUT                    00 EXPECTED            *IX1094.2
076200*         IX-3, 1.3.4 (1) A                                      *IX1094.2
076300******************************************************************IX1094.2
076400 CLO-INIT-GF-01-0.                                                IX1094.2
076500     MOVE SPACES TO IX-FS3-STATUS.                                IX1094.2
076600     MOVE "CLOSE OUTPUT:00 EXP." TO FEATURE.                      IX1094.2
076700     MOVE "CLO-TEST-GF-01-0" TO PAR-NAME.                         IX1094.2
076800 CLO-TEST-GF-01-0.                                                IX1094.2
076900     CLOSE IX-FS3.                                                IX1094.2
077000     IF IX-FS3-STATUS = "00"                                      IX1094.2
077100         GO TO CLO-PASS-GF-01-0.                                  IX1094.2
077200 CLO-FAIL-GF-01-0.                                                IX1094.2
077300     MOVE "IX-3, 1.3.4, (1) A. " TO RE-MARK.                      IX1094.2
077400     PERFORM FAIL.                                                IX1094.2
077500     MOVE IX-FS3-STATUS TO COMPUTED-A.                            IX1094.2
077600     MOVE "00" TO CORRECT-X.                                      IX1094.2
077700     GO TO CLO-WRITE-GF-01-0.                                     IX1094.2
077800 CLO-PASS-GF-01-0.                                                IX1094.2
077900     PERFORM PASS.                                                IX1094.2
078000 CLO-WRITE-GF-01-0.                                               IX1094.2
078100     PERFORM PRINT-DETAIL.                                        IX1094.2
078200                                                                  IX1094.2
078300******************************************************************IX1094.2
078400*    A INDEXED FILE WITH 50 RECORDS HAS BEEN CREATED.            *IX1094.2
078500******************************************************************IX1094.2
078600                                                                  IX1094.2
078700******************************************************************IX1094.2
078800*   TEST  5                                                      *IX1094.2
078900*         OPEN INPUT                      00 EXPECTED            *IX1094.2
079000*         IX-3, 1.3.4 (1) A                                      *IX1094.2
079100******************************************************************IX1094.2
079200 OPN-INIT-GF-02-0.                                                IX1094.2
079300     MOVE SPACES TO IX-FS3-STATUS.                                IX1094.2
079400     MOVE "OPEN INPUT:  00 EXP." TO FEATURE.                      IX1094.2
079500     MOVE "OPN-TEST-GF-02-0" TO PAR-NAME.                         IX1094.2
079600 OPN-TEST-GF-02-0.                                                IX1094.2
079700     OPEN                                                         IX1094.2
079800        INPUT IX-FS3.                                             IX1094.2
079900     IF IX-FS3-STATUS EQUAL TO "00"                               IX1094.2
080000         GO TO OPN-PASS-GF-02-0.                                  IX1094.2
080100 OPN-FAIL-GF-02-0.                                                IX1094.2
080200     MOVE "IX-3, 1.3.4, (1) A." TO RE-MARK.                       IX1094.2
080300     PERFORM FAIL.                                                IX1094.2
080400     MOVE IX-FS3-STATUS TO COMPUTED-A.                            IX1094.2
080500     MOVE "00" TO CORRECT-X.                                      IX1094.2
080600     GO TO OPN-WRITE-GF-02-0.                                     IX1094.2
080700 OPN-PASS-GF-02-0.                                                IX1094.2
080800     PERFORM PASS.                                                IX1094.2
080900 OPN-WRITE-GF-02-0.                                               IX1094.2
081000     PERFORM PRINT-DETAIL.                                        IX1094.2
081100******************************************************************IX1094.2
081200*    STATUS 10 CHECK ON INPUT FILE IX-FS3.                       *IX1094.2
081300*    THIS TEST READS AND VERIFIES THE RECORDS WRITTEN IN         *IX1094.2
081400*    INX-TEST-004. THE USE ON INPUT PROCESSES THE AT END         *IX1094.2
081500*    CONDITION. THERE IS NO AT END PHRASE IN THE READ STATEMENT. *IX1094.2
081600******************************************************************IX1094.2
081700 REA-INIT-F1-01-0.                                                IX1094.2
081800     MOVE 1 TO STATUS-TEST-10.                                    IX1094.2
081900     MOVE SPACES TO IX-FS3-STATUS.                                IX1094.2
082000     MOVE ZERO TO COUNT-OF-RECS.                                  IX1094.2
082100     MOVE ZERO TO RECORDS-IN-ERROR.                               IX1094.2
082200     MOVE ZERO TO PERM-ERRORS.                                    IX1094.2
082300     MOVE ZERO TO EOF-FLAG.                                       IX1094.2
082400 REA-TEST-F1-01-0.                                                IX1094.2
082500     READ IX-FS3.                                                 IX1094.2
082600     IF EOF-FLAG EQUAL TO 1                                       IX1094.2
082700         GO TO REA-TEST-F1-01-1.                                  IX1094.2
082800     MOVE IX-FS3R1-F-G-240 TO FILE-RECORD-INFO (1).               IX1094.2
082900     ADD 1 TO COUNT-OF-RECS.                                      IX1094.2
083000     IF COUNT-OF-RECS GREATER THAN 50                             IX1094.2
083100         MOVE "MORE THAN 50 RECORDS" TO RE-MARK                   IX1094.2
083200         GO TO REA-FAIL-F1-01-0.                                  IX1094.2
083300     IF COUNT-OF-RECS LESS THAN 51                                IX1094.2
083400        IF IX-FS3-STATUS NOT = "00"                               IX1094.2
083500            MOVE 1 TO STATUS-TEST-READ.                           IX1094.2
083600     IF PERM-ERRORS EQUAL TO 1                                    IX1094.2
083700         ADD 1 TO RECORDS-IN-ERROR                                IX1094.2
083800         GO TO REA-TEST-F1-01-1.                                  IX1094.2
083900     IF COUNT-OF-RECS NOT EQUAL TO XRECORD-NUMBER (1)             IX1094.2
084000         ADD 1 TO RECORDS-IN-ERROR                                IX1094.2
084100         GO TO REA-TEST-F1-01-1.                                  IX1094.2
084200     IF XFILE-NAME (1) NOT EQUAL TO "IX-FS3"                      IX1094.2
084300         ADD 1 TO RECORDS-IN-ERROR                                IX1094.2
084400         GO TO REA-TEST-F1-01-1.                                  IX1094.2
084500     MOVE XRECORD-KEY (1) TO GRP-0101.                            IX1094.2
084600     IF GRP-0101-KEY NOT EQUAL TO COUNT-OF-RECS                   IX1094.2
084700         ADD 1 TO RECORDS-IN-ERROR.                               IX1094.2
084800     GO TO REA-TEST-F1-01-0.                                      IX1094.2
084900 REA-TEST-F1-01-1.                                                IX1094.2
085000******************************************************************IX1094.2
085100*   TEST  6                                                      *IX1094.2
085200*         READ ... .   (WITHOUT   AT END) 00 EXPECTED            *IX1094.2
085300*         IX-3, 1.3.4 (1) A                                      *IX1094.2
085400******************************************************************IX1094.2
085500     MOVE "REA-TEST-F1-01-0" TO PAR-NAME.                         IX1094.2
085600     MOVE "READ (USE):  00 EXP."     TO FEATURE.                  IX1094.2
085700     IF STATUS-TEST-READ = 0                                      IX1094.2
085800         GO TO REA-PASS-F1-01-0.                                  IX1094.2
085900 REA-FAIL-F1-01-0.                                                IX1094.2
086000     MOVE "IX-3, 1.3.4, (1) A. " TO RE-MARK.                      IX1094.2
086100     MOVE "I-O STATUS IS NOT 00" TO COMPUTED-A.                   IX1094.2
086200     MOVE "00" TO CORRECT-X.                                      IX1094.2
086300     PERFORM FAIL.                                                IX1094.2
086400     GO TO REA-WRITE-F1-01-0.                                     IX1094.2
086500 REA-PASS-F1-01-0.                                                IX1094.2
086600     PERFORM PASS.                                                IX1094.2
086700 REA-WRITE-F1-01-0.                                               IX1094.2
086800     PERFORM PRINT-DETAIL.                                        IX1094.2
086900*                                                                 IX1094.2
087000 REA-INIT-GF-02-0.                                                IX1094.2
087100******************************************************************IX1094.2
087200*   TEST  7                                                      *IX1094.2
087300*         VERIFY FILE                                            *IX1094.2
087400******************************************************************IX1094.2
087500     IF RECORDS-IN-ERROR EQUAL TO ZERO                            IX1094.2
087600         GO TO REA-TEST-F1-02-0.                                  IX1094.2
087700     MOVE "ERRORS IN READING IX-FS3" TO RE-MARK.                  IX1094.2
087800 REA-FAIL-F1-02-0.                                                IX1094.2
087900     MOVE "RECORDS IN ERROR =" TO COMPUTED-A.                     IX1094.2
088000     MOVE RECORDS-IN-ERROR TO CORRECT-18V0.                       IX1094.2
088100     GO TO REA-FAIL-F1-02-1.                                      IX1094.2
088200 REA-TEST-F1-02-0.                                                IX1094.2
088300     IF COUNT-OF-RECORDS EQUAL TO 50                              IX1094.2
088400         GO TO REA-PASS-F1-02-0.                                  IX1094.2
088500     MOVE "UNEXPECTED EOF" TO RE-MARK.                            IX1094.2
088600     MOVE "RECORDS READ =" TO COMPUTED-A.                         IX1094.2
088700     MOVE COUNT-OF-RECORDS TO CORRECT-18V0.                       IX1094.2
088800 REA-FAIL-F1-02-1.                                                IX1094.2
088900     PERFORM FAIL.                                                IX1094.2
089000     GO TO REA-WRITE-F1-02-0.                                     IX1094.2
089100 REA-PASS-F1-02-0.                                                IX1094.2
089200     PERFORM PASS.                                                IX1094.2
089300     MOVE "FILE VERIFIED RECS =" TO COMPUTED-A.                   IX1094.2
089400     MOVE COUNT-OF-RECORDS TO CORRECT-18V0.                       IX1094.2
089500 REA-WRITE-F1-02-0.                                               IX1094.2
089600     MOVE "REA-TEST-F1-02-0" TO PAR-NAME.                         IX1094.2
089700     MOVE "VERIFY FILE IX-FS3" TO FEATURE.                        IX1094.2
089800     PERFORM PRINT-DETAIL.                                        IX1094.2
089900******************************************************************IX1094.2
090000*   TEST  8                                                      *IX1094.2
090100*         READ.   (WITHOUT   AT END)      10 EXPECTED            *IX1094.2
090200*         IX-4, 1.3.4 (2) A                                      *IX1094.2
090300******************************************************************IX1094.2
090400 REA-INIT-F1-03-0.                                                IX1094.2
090500     IF IX-FS3-STATUS EQUAL TO "10"                               IX1094.2
090600         GO TO REA-PASS-F1-03-0.                                  IX1094.2
090700 REA-FAIL-F1-03-0.                                                IX1094.2
090800     MOVE IX-FS3-STATUS TO COMPUTED-A.                            IX1094.2
090900     MOVE "10" TO CORRECT-X.                                      IX1094.2
091000     MOVE "IX-4, 1.3.4, (2) A." TO RE-MARK.                       IX1094.2
091100     PERFORM FAIL.                                                IX1094.2
091200     GO TO REA-WRITE-F1-03-0.                                     IX1094.2
091300 REA-PASS-F1-03-0.                                                IX1094.2
091400     PERFORM PASS.                                                IX1094.2
091500 REA-WRITE-F1-03-0.                                               IX1094.2
091600     MOVE "READ :       10 EXP." TO FEATURE.                      IX1094.2
091700     MOVE "REA-TEST-F1-03-0" TO PAR-NAME.                         IX1094.2
091800     PERFORM PRINT-DETAIL.                                        IX1094.2
091900******************************************************************IX1094.2
092000*   TEST  9                                                      *IX1094.2
092100*         CLOSE INPUT                     00 EXPECTED            *IX1094.2
092200*         IX-3, 1.3.4 (1) A                                      *IX1094.2
092300******************************************************************IX1094.2
092400 CLO-TEST-GF-02-0.                                                IX1094.2
092500     MOVE SPACES TO IX-FS3-STATUS.                                IX1094.2
092600     MOVE "CLOSE INPUT: 00 EXP."  TO FEATURE.                     IX1094.2
092700     MOVE "CLO-TEST-GF-02-0" TO PAR-NAME.                         IX1094.2
092800     CLOSE IX-FS3.                                                IX1094.2
092900     IF IX-FS3-STATUS = "00"                                      IX1094.2
093000         GO TO CLO-PASS-GF-02-0.                                  IX1094.2
093100 CLO-FAIL-GF-02-0.                                                IX1094.2
093200     MOVE "IX-3, 1.3.4, (1) A    " TO RE-MARK.                    IX1094.2
093300     MOVE IX-FS3-STATUS TO COMPUTED-A.                            IX1094.2
093400     MOVE "00" TO CORRECT-X.                                      IX1094.2
093500     PERFORM FAIL.                                                IX1094.2
093600     GO TO CLO-WRITE-GF-02-0.                                     IX1094.2
093700 CLO-PASS-GF-02-0.                                                IX1094.2
093800     PERFORM PASS.                                                IX1094.2
093900 CLO-WRITE-GF-02-0.                                               IX1094.2
094000     PERFORM PRINT-DETAIL.                                        IX1094.2
094100                                                                  IX1094.2
094200******************************************************************IX1094.2
094300*   TEST  10                                                     *IX1094.2
094400*         OPEN INPUT   (FOR READ ...  AT END) 00 EXPECTED        *IX1094.2
094500*         IX-3, 1.3.4 (1) A                                      *IX1094.2
094600******************************************************************IX1094.2
094700 OPN-INIT-GF-03-0.                                                IX1094.2
094800     MOVE SPACES TO IX-FS3-STATUS.                                IX1094.2
094900     MOVE "OPEN  INPUT: 00 EXP." TO FEATURE.                      IX1094.2
095000     MOVE "OPN-TEST-GF-03-0" TO PAR-NAME.                         IX1094.2
095100 OPN-TEST-GF-03-0.                                                IX1094.2
095200     OPEN                                                         IX1094.2
095300        INPUT IX-FS3.                                             IX1094.2
095400     IF IX-FS3-STATUS EQUAL TO "00"                               IX1094.2
095500         GO TO OPN-PASS-GF-03-0.                                  IX1094.2
095600 OPN-FAIL-GF-03-0.                                                IX1094.2
095700     MOVE "IX-3, 1.3.4, (1) A." TO RE-MARK.                       IX1094.2
095800     PERFORM FAIL.                                                IX1094.2
095900     MOVE IX-FS3-STATUS TO COMPUTED-A.                            IX1094.2
096000     MOVE "00" TO CORRECT-X.                                      IX1094.2
096100     GO TO OPN-WRITE-GF-03-0.                                     IX1094.2
096200 OPN-PASS-GF-03-0.                                                IX1094.2
096300     PERFORM PASS.                                                IX1094.2
096400 OPN-WRITE-GF-03-0.                                               IX1094.2
096500     PERFORM PRINT-DETAIL.                                        IX1094.2
096600******************************************************************IX1094.2
096700*    STATUS IO CHECK ON INPUT FILE IX-FS3.                       *IX1094.2
096800*    THIS TEST READS AND VERIFIES THE RECORDS WRITTEN IN         *IX1094.2
096900*    TEST 2.       THE USE ON INPUT PROCESSES THE AT END         *IX1094.2
097000*    CONDITION.                                                   IX1094.2
097100******************************************************************IX1094.2
097200 REA-INIT-F1-04-0.                                                IX1094.2
097300     MOVE 1 TO STATUS-TEST-10.                                    IX1094.2
097400     MOVE ZERO TO STATUS-TEST-READ.                               IX1094.2
097500     MOVE SPACES TO IX-FS3-STATUS.                                IX1094.2
097600     MOVE ZERO TO COUNT-OF-RECS.                                  IX1094.2
097700     MOVE ZERO TO PERM-ERRORS.                                    IX1094.2
097800     MOVE ZERO TO EOF-FLAG.                                       IX1094.2
097900 REA-TEST-F1-04-0.                                                IX1094.2
098000     READ IX-FS3  AT END MOVE 1 TO EOF-FLAG.                      IX1094.2
098100     IF EOF-FLAG EQUAL TO 1                                       IX1094.2
098200         GO TO REA-TEST-F1-04-1.                                  IX1094.2
098300     MOVE IX-FS3R1-F-G-240 TO FILE-RECORD-INFO (1).               IX1094.2
098400     ADD 1 TO COUNT-OF-RECS.                                      IX1094.2
098500     IF COUNT-OF-RECS LESS THAN 51                                IX1094.2
098600        IF IX-FS3-STATUS NOT = "00"                               IX1094.2
098700            MOVE 1 TO STATUS-TEST-READ.                           IX1094.2
098800     IF PERM-ERRORS EQUAL TO 1                                    IX1094.2
098900         ADD 1 TO RECORDS-IN-ERROR                                IX1094.2
099000         GO TO REA-TEST-F1-04-1.                                  IX1094.2
099100     IF COUNT-OF-RECS NOT EQUAL TO XRECORD-NUMBER (1)             IX1094.2
099200         ADD 1 TO RECORDS-IN-ERROR                                IX1094.2
099300         GO TO REA-TEST-F1-04-1.                                  IX1094.2
099400     IF XFILE-NAME (1) NOT EQUAL TO "IX-FS3"                      IX1094.2
099500         ADD 1 TO RECORDS-IN-ERROR                                IX1094.2
099600         GO TO REA-TEST-F1-04-1.                                  IX1094.2
099700     MOVE XRECORD-KEY (1) TO GRP-0101.                            IX1094.2
099800     IF GRP-0101-KEY NOT EQUAL TO COUNT-OF-RECS                   IX1094.2
099900         ADD 1 TO RECORDS-IN-ERROR.                               IX1094.2
100000     GO TO REA-TEST-F1-04-0.                                      IX1094.2
100100 REA-TEST-F1-04-1.                                                IX1094.2
100200******************************************************************IX1094.2
100300*   TEST  11                                                     *IX1094.2
100400*         READ ...  AT END                00 EXPECTED            *IX1094.2
100500*         IX-3, 1.3.4 (1) A                                      *IX1094.2
100600******************************************************************IX1094.2
100700     MOVE "REA-TEST-F1-04-0" TO PAR-NAME.                         IX1094.2
100800     MOVE "READ...END:  00 EXP."     TO FEATURE.                  IX1094.2
100900     IF STATUS-TEST-READ = 0                                      IX1094.2
101000         GO TO REA-PASS-F1-04-0.                                  IX1094.2
101100 REA-FAIL-F1-04-0.                                                IX1094.2
101200     MOVE "I-O STATUS IS NOT 00" TO COMPUTED-A.                   IX1094.2
101300     MOVE "00" TO CORRECT-X.                                      IX1094.2
101400     MOVE "IX-3, 1.3.4, (1) A. " TO RE-MARK.                      IX1094.2
101500     PERFORM FAIL.                                                IX1094.2
101600     GO TO REA-WRITE-F1-04-0.                                     IX1094.2
101700 REA-PASS-F1-04-0.                                                IX1094.2
101800     PERFORM PASS.                                                IX1094.2
101900 REA-WRITE-F1-04-0.                                               IX1094.2
102000     PERFORM PRINT-DETAIL.                                        IX1094.2
102100*                                                                 IX1094.2
102200 REA-TEST-F1-05-0.                                                IX1094.2
102300******************************************************************IX1094.2
102400*   TEST  12                                                     *IX1094.2
102500*         READ ... AT END    10 EXPECTED                         *IX1094.2
102600*         IX-4, 1.3.4 (2) A 1)                                   *IX1094.2
102700******************************************************************IX1094.2
102800     IF IX-FS3-STATUS EQUAL TO "10"                               IX1094.2
102900         GO TO REA-PASS-F1-05-0.                                  IX1094.2
103000 REA-FAIL-F1-05-0.                                                IX1094.2
103100     MOVE IX-FS3-STATUS TO COMPUTED-A.                            IX1094.2
103200     MOVE "10" TO CORRECT-X.                                      IX1094.2
103300     MOVE "IX-4, 1.3.4, (2) A 1)" TO RE-MARK.                     IX1094.2
103400     PERFORM FAIL.                                                IX1094.2
103500     GO TO REA-WRITE-F1-05-0.                                     IX1094.2
103600 REA-PASS-F1-05-0.                                                IX1094.2
103700     PERFORM PASS.                                                IX1094.2
103800 REA-WRITE-F1-05-0.                                               IX1094.2
103900     MOVE "READ...END:  10 EXP." TO FEATURE.                      IX1094.2
104000     MOVE "REA-TEST-F1-05-0" TO PAR-NAME.                         IX1094.2
104100     PERFORM PRINT-DETAIL.                                        IX1094.2
104200******************************************************************IX1094.2
104300*   TEST  13                                                     *IX1094.2
104400*         READ ... (AFTER AT END)      46 EXPECTED               *IX1094.2
104500*         IX-5, 1.3.4 (5) E 3)                                   *IX1094.2
104600******************************************************************IX1094.2
104700 REA-TEST-F1-06-0.                                                IX1094.2
104800     MOVE 13 TO TEST-NO.                                          IX1094.2
104900     READ IX-FS3 AT END GO TO REA-TEST-F1-06-1.                   IX1094.2
105000 REA-TEST-F1-06-1.                                                IX1094.2
105100     IF IX-FS3-STATUS EQUAL TO "46"                               IX1094.2
105200        MOVE "SHOULD HAVE EXECUTED DECLARATIVES IX-3,1.3.4(4)"    IX1094.2
105300          TO RE-MARK                                              IX1094.2
105400        GO TO REA-WRITE-F1-06-0.                                  IX1094.2
105500 REA-FAIL-F1-06-0.                                                IX1094.2
105600     MOVE "IX-5, 1.3.4, (5) E 3)" TO RE-MARK.                     IX1094.2
105700 REA-WRITE-F1-06-0.                                               IX1094.2
105800     MOVE IX-FS3-STATUS TO COMPUTED-A.                            IX1094.2
105900     MOVE "46" TO CORRECT-X.                                      IX1094.2
106000     PERFORM FAIL.                                                IX1094.2
106100     MOVE "READ.        46 EXP." TO FEATURE.                      IX1094.2
106200     MOVE "REA-TEST-F1-06-0" TO PAR-NAME.                         IX1094.2
106300     PERFORM PRINT-DETAIL.                                        IX1094.2
106400     CLOSE IX-FS3.                                                IX1094.2
106500                                                                  IX1094.2
106600 TERMINATE-ROUTINE.                                               IX1094.2
106700     EXIT.                                                        IX1094.2
106800                                                                  IX1094.2
106900 CCVS-EXIT SECTION.                                               IX1094.2
107000 CCVS-999999.                                                     IX1094.2
107100     GO TO CLOSE-FILES.                                           IX1094.2
      *END-OF,IX109A                                                                  
