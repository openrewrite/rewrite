      *HEADER,COBOL,IX112A                                                            
000100 IDENTIFICATION DIVISION.                                         IX1124.2
000200 PROGRAM-ID.                                                      IX1124.2
000300     IX112A.                                                      IX1124.2
000400****************************************************************  IX1124.2
000500*                                                              *  IX1124.2
000600*    VALIDATION FOR:-                                          *  IX1124.2
000700*                                                              *  IX1124.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IX1124.2
000900*                                                              *  IX1124.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".IX1124.2
001100*                                                              *  IX1124.2
001200****************************************************************  IX1124.2
001300*                                                                 IX1124.2
001400*    1.  THE  ROUTINE  CREATES  THE  MASS  STORAGE  FILE  IX-VS2  IX1124.2
001500*        CONTAINING  50  RECORDS. EACH BLOCK CONTAINS 2 RECORDS,  IX1124.2
001600*        EACH  RECORD  CONTAINS  240 CHARACTERS, ORGANIZATION IS  IX1124.2
001700*        INDEXED,  ACCESS  IS SEQUENTIAL. THEN THE FILE IS OPENED IX1124.2
001800*        AS I-O AND IT IS ATTEMTED TO REWRITE A RECORD WITH A     IX1124.2
001900*        WRONG RECORD LENGTH.                                     IX1124.2
002000*                                                                 IX1124.2
002100*    2. THE ROUTINE READS THE CREATED FILE, VERIFIES IT AND       IX1124.2
002200*       CHECKS THE FILE STATUS CODE:                              IX1124.2
002300*           44  -  AFTER REWRITE   (WITH WRONG RECORD LENGTH)     IX1124.2
002400*                                                                 IX1124.2
002500*    3. X-CARDS USED IN THIS PROGRAM:                             IX1124.2
002600*                                                                 IX1124.2
002700*                 XXXXX024                                        IX1124.2
002800*                 XXXXX055.                                       IX1124.2
002900*                 XXXXX062.                                       IX1124.2
003000*                 XXXXX082.                                       IX1124.2
003100*                 XXXXX083.                                       IX1124.2
003200*                 XXXXX084                                        IX1124.2
003300*                                                                 IX1124.2
003400*                                                                 IX1124.2
003500 ENVIRONMENT DIVISION.                                            IX1124.2
003600 CONFIGURATION SECTION.                                           IX1124.2
003700 SOURCE-COMPUTER.                                                 IX1124.2
003800     XXXXX082.                                                    IX1124.2
003900 OBJECT-COMPUTER.                                                 IX1124.2
004000     XXXXX083.                                                    IX1124.2
004100 INPUT-OUTPUT SECTION.                                            IX1124.2
004200 FILE-CONTROL.                                                    IX1124.2
004300*    SELECT RAW-DATA   ASSIGN TO                                  IX1124.2
004400*    XXXXX062                                                     IX1124.2
004500*           ORGANIZATION IS INDEXED                               IX1124.2
004600*           ACCESS MODE IS RANDOM                                 IX1124.2
004700*           RECORD KEY IS RAW-DATA-KEY.                           IX1124.2
004800*                                                                 IX1124.2
004900     SELECT PRINT-FILE ASSIGN TO                                  IX1124.2
005000     XXXXX055.                                                    IX1124.2
005100*                                                                 IX1124.2
005200     SELECT IX-VS2 ASSIGN                                         IX1124.2
005300     XXXXX024                                                     IX1124.2
005400     ORGANIZATION IS INDEXED                                      IX1124.2
005500     ACCESS MODE IS SEQUENTIAL                                    IX1124.2
005600     RECORD KEY IS IX-VS2-KEY                                     IX1124.2
005700     FILE STATUS IS IX-VS2-STATUS.                                IX1124.2
005800                                                                  IX1124.2
005900 DATA DIVISION.                                                   IX1124.2
006000                                                                  IX1124.2
006100 FILE SECTION.                                                    IX1124.2
006200*                                                                 IX1124.2
006300*FD  RAW-DATA.                                                    IX1124.2
006400*                                                                 IX1124.2
006500*01  RAW-DATA-SATZ.                                               IX1124.2
006600*    05  RAW-DATA-KEY        PIC X(6).                            IX1124.2
006700*    05  C-DATE              PIC 9(6).                            IX1124.2
006800*    05  C-TIME              PIC 9(8).                            IX1124.2
006900*    05  C-NO-OF-TESTS       PIC 99.                              IX1124.2
007000*    05  C-OK                PIC 999.                             IX1124.2
007100*    05  C-ALL               PIC 999.                             IX1124.2
007200*    05  C-FAIL              PIC 999.                             IX1124.2
007300*    05  C-DELETED           PIC 999.                             IX1124.2
007400*    05  C-INSPECT           PIC 999.                             IX1124.2
007500*    05  C-NOTE              PIC X(13).                           IX1124.2
007600*    05  C-INDENT            PIC X.                               IX1124.2
007700*    05  C-ABORT             PIC X(8).                            IX1124.2
007800                                                                  IX1124.2
007900 FD  PRINT-FILE.                                                  IX1124.2
008000                                                                  IX1124.2
008100 01  PRINT-REC               PIC X(120).                          IX1124.2
008200                                                                  IX1124.2
008300 01  DUMMY-RECORD            PIC X(120).                          IX1124.2
008400                                                                  IX1124.2
008500 FD  IX-VS2                                                       IX1124.2
008600C       DATA RECORDS IX-VS2R1-F-G-240 IX-VS2R1-F-G-200            IX1124.2
008700C                                     IX-VS2R1-F-G-280            IX1124.2
008800C       LABEL RECORD STANDARD                                     IX1124.2
008900        RECORD 200 TO 280                                         IX1124.2
009000        BLOCK CONTAINS 2 RECORDS.                                 IX1124.2
009100                                                                  IX1124.2
009200 01  IX-VS2R1-F-G-240.                                            IX1124.2
009300     05  IX-VS2-REC-120      PIC X(120).                          IX1124.2
009400     05  IX-VS2-REC-120-240.                                      IX1124.2
009500         10  FILLER          PIC X(8).                            IX1124.2
009600         10  IX-VS2-KEY      PIC X(29).                           IX1124.2
009700         10  FILLER          PIC X(9).                            IX1124.2
009800         10  IX-VS2-ALTER-KEY  PIC X(29).                         IX1124.2
009900         10  FILLER            PIC X(45).                         IX1124.2
010000                                                                  IX1124.2
010100 01  IX-VS2R1-F-G-200.                                            IX1124.2
010200     05  IX-VS2-REC-SHORT    PIC X(120).                          IX1124.2
010300     05  IX-VS2-REC-120-200  PIC X(80).                           IX1124.2
010400                                                                  IX1124.2
010500 01  IX-VS2R1-F-G-280.                                            IX1124.2
010600     05  IX-VS2-REC-LONG     PIC X(120).                          IX1124.2
010700     05  IX-VS2-REC-120-239  PIC X(120).                          IX1124.2
010800     05  IX-VS2-REC-240-280  PIC X(40).                           IX1124.2
010900                                                                  IX1124.2
011000 WORKING-STORAGE SECTION.                                         IX1124.2
011100                                                                  IX1124.2
011200 01  GRP-0101.                                                    IX1124.2
011300     05  FILLER              PIC X(10) VALUE "RECORD-KEY".        IX1124.2
011400     05  GRP-0101-KEY        PIC 9(9)  VALUE ZERO.                IX1124.2
011500     05  FILLER              PIC X(10) VALUE "END-OF-KEY".        IX1124.2
011600                                                                  IX1124.2
011700 01  GRP-0102.                                                    IX1124.2
011800     05  FILLER              PIC X(10) VALUE "ALTERN-KEY".        IX1124.2
011900     05  GRP-0102-KEY        PIC 9(9)  VALUE ZERO.                IX1124.2
012000     05  FILLER              PIC X(10) VALUE "END-AL-KEY".        IX1124.2
012100                                                                  IX1124.2
012200 01  WRK-CS-09V00            PIC S9(9) COMP VALUE ZERO.           IX1124.2
012300                                                                  IX1124.2
012400 01  EOF-FLAG                PIC 9 VALUE ZERO.                    IX1124.2
012500                                                                  IX1124.2
012600 01  RECORDS-IN-ERROR        PIC S9(5) COMP VALUE ZERO.           IX1124.2
012700                                                                  IX1124.2
012800 01  ERROR-FLAG              PIC 9 VALUE ZERO.                    IX1124.2
012900                                                                  IX1124.2
013000 01  PERM-ERRORS             PIC S9(5) COMP VALUE ZERO.           IX1124.2
013100                                                                  IX1124.2
013200 01  STATUS-TEST-00          PIC 9 VALUE ZERO.                    IX1124.2
013300                                                                  IX1124.2
013400 01  STATUS-TEST-10          PIC 9 VALUE ZERO.                    IX1124.2
013500 01  STATUS-TEST-READ        PIC 9 VALUE ZERO.                    IX1124.2
013600                                                                  IX1124.2
013700 01  IX-VS2-STATUS.                                               IX1124.2
013800     05  IX-VS2-STAT1        PIC X.                               IX1124.2
013900     05  IX-VS2-STAT2        PIC X.                               IX1124.2
014000                                                                  IX1124.2
014100 01  COUNT-OF-RECS           PIC 9(5).                            IX1124.2
014200                                                                  IX1124.2
014300 01  COUNT-OF-RECORDS REDEFINES COUNT-OF-RECS  PIC 9(5).          IX1124.2
014400                                                                  IX1124.2
014500 01  FILE-RECORD-INFORMATION-REC.                                 IX1124.2
014600     05  FILE-RECORD-INFO-SKELETON.                               IX1124.2
014700         10  FILLER          PIC X(48) VALUE                      IX1124.2
014800              "FILE=      ,RECORD=      /0,RECNO=000000,UPDT=00". IX1124.2
014900         10  FILLER          PIC X(46) VALUE                      IX1124.2
015000                ",ODO=0000,PGM=     ,LRECL=000000,BLKSIZ  =0000". IX1124.2
015100         10  FILLER          PIC X(26) VALUE                      IX1124.2
015200                                    ",LFIL=000000,ORG=  ,LBLR= ". IX1124.2
015300         10  FILLER          PIC X(37) VALUE                      IX1124.2
015400                         ",RECKEY=                             ". IX1124.2
015500         10  FILLER          PIC X(38) VALUE                      IX1124.2
015600                        ",ALTKEY1=                             ". IX1124.2
015700         10  FILLER          PIC X(38) VALUE                      IX1124.2
015800                        ",ALTKEY2=                             ". IX1124.2
015900         10  FILLER          PIC X(7) VALUE SPACE.                IX1124.2
016000     05  FILE-RECORD-INFO             OCCURS 10.                  IX1124.2
016100         10  FILE-RECORD-INFO-P1-120.                             IX1124.2
016200             15  FILLER      PIC X(5).                            IX1124.2
016300             15  XFILE-NAME  PIC X(6).                            IX1124.2
016400             15  FILLER      PIC X(8).                            IX1124.2
016500             15  XRECORD-NAME  PIC X(6).                          IX1124.2
016600             15  FILLER        PIC X(1).                          IX1124.2
016700             15  REELUNIT-NUMBER  PIC 9(1).                       IX1124.2
016800             15  FILLER           PIC X(7).                       IX1124.2
016900             15  XRECORD-NUMBER   PIC 9(6).                       IX1124.2
017000             15  FILLER           PIC X(6).                       IX1124.2
017100             15  UPDATE-NUMBER    PIC 9(2).                       IX1124.2
017200             15  FILLER           PIC X(5).                       IX1124.2
017300             15  ODO-NUMBER       PIC 9(4).                       IX1124.2
017400             15  FILLER           PIC X(5).                       IX1124.2
017500             15  XPROGRAM-NAME    PIC X(5).                       IX1124.2
017600             15  FILLER           PIC X(7).                       IX1124.2
017700             15  XRECORD-LENGTH   PIC 9(6).                       IX1124.2
017800             15  FILLER           PIC X(7).                       IX1124.2
017900             15  CHARS-OR-RECORDS  PIC X(2).                      IX1124.2
018000             15  FILLER            PIC X(1).                      IX1124.2
018100             15  XBLOCK-SIZE       PIC 9(4).                      IX1124.2
018200             15  FILLER            PIC X(6).                      IX1124.2
018300             15  RECORDS-IN-FILE   PIC 9(6).                      IX1124.2
018400             15  FILLER            PIC X(5).                      IX1124.2
018500             15  XFILE-ORGANIZATION  PIC X(2).                    IX1124.2
018600             15  FILLER              PIC X(6).                    IX1124.2
018700             15  XLABEL-TYPE         PIC X(1).                    IX1124.2
018800         10  FILE-RECORD-INFO-P121-240.                           IX1124.2
018900             15  FILLER              PIC X(8).                    IX1124.2
019000             15  XRECORD-KEY         PIC X(29).                   IX1124.2
019100             15  FILLER              PIC X(9).                    IX1124.2
019200             15  ALTERNATE-KEY1      PIC X(29).                   IX1124.2
019300             15  FILLER              PIC X(9).                    IX1124.2
019400             15  ALTERNATE-KEY2      PIC X(29).                   IX1124.2
019500             15  FILLER              PIC X(7).                    IX1124.2
019600                                                                  IX1124.2
019700 01  TEST-RESULTS.                                                IX1124.2
019800     02 FILLER                   PIC X      VALUE SPACE.          IX1124.2
019900     02 FEATURE                  PIC X(20)  VALUE SPACE.          IX1124.2
020000     02 FILLER                   PIC X      VALUE SPACE.          IX1124.2
020100     02 P-OR-F                   PIC X(5)   VALUE SPACE.          IX1124.2
020200     02 FILLER                   PIC X      VALUE SPACE.          IX1124.2
020300     02  PAR-NAME.                                                IX1124.2
020400       03 FILLER                 PIC X(19)  VALUE SPACE.          IX1124.2
020500       03  PARDOT-X              PIC X      VALUE SPACE.          IX1124.2
020600       03 DOTVALUE               PIC 99     VALUE ZERO.           IX1124.2
020700     02 FILLER                   PIC X(8)   VALUE SPACE.          IX1124.2
020800     02 RE-MARK                  PIC X(61).                       IX1124.2
020900 01  TEST-COMPUTED.                                               IX1124.2
021000     02 FILLER                   PIC X(30)  VALUE SPACE.          IX1124.2
021100     02 FILLER                   PIC X(17)  VALUE                 IX1124.2
021200            "       COMPUTED=".                                   IX1124.2
021300     02 COMPUTED-X.                                               IX1124.2
021400     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          IX1124.2
021500     03 COMPUTED-N               REDEFINES COMPUTED-A             IX1124.2
021600                                 PIC -9(9).9(9).                  IX1124.2
021700     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         IX1124.2
021800     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     IX1124.2
021900     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     IX1124.2
022000     03       CM-18V0 REDEFINES COMPUTED-A.                       IX1124.2
022100         04 COMPUTED-18V0                    PIC -9(18).          IX1124.2
022200         04 FILLER                           PIC X.               IX1124.2
022300     03 FILLER PIC X(50) VALUE SPACE.                             IX1124.2
022400 01  TEST-CORRECT.                                                IX1124.2
022500     02 FILLER PIC X(30) VALUE SPACE.                             IX1124.2
022600     02 FILLER PIC X(17) VALUE "       CORRECT =".                IX1124.2
022700     02 CORRECT-X.                                                IX1124.2
022800     03 CORRECT-A                  PIC X(20) VALUE SPACE.         IX1124.2
022900     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      IX1124.2
023000     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         IX1124.2
023100     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     IX1124.2
023200     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     IX1124.2
023300     03      CR-18V0 REDEFINES CORRECT-A.                         IX1124.2
023400         04 CORRECT-18V0                     PIC -9(18).          IX1124.2
023500         04 FILLER                           PIC X.               IX1124.2
023600     03 FILLER PIC X(2) VALUE SPACE.                              IX1124.2
023700     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     IX1124.2
023800 01  CCVS-C-1.                                                    IX1124.2
023900     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PAIX1124.2
024000-    "SS  PARAGRAPH-NAME                                          IX1124.2
024100-    "       REMARKS".                                            IX1124.2
024200     02 FILLER                     PIC X(20)    VALUE SPACE.      IX1124.2
024300 01  CCVS-C-2.                                                    IX1124.2
024400     02 FILLER                     PIC X        VALUE SPACE.      IX1124.2
024500     02 FILLER                     PIC X(6)     VALUE "TESTED".   IX1124.2
024600     02 FILLER                     PIC X(15)    VALUE SPACE.      IX1124.2
024700     02 FILLER                     PIC X(4)     VALUE "FAIL".     IX1124.2
024800     02 FILLER                     PIC X(94)    VALUE SPACE.      IX1124.2
024900 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       IX1124.2
025000 01  REC-CT                        PIC 99       VALUE ZERO.       IX1124.2
025100 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       IX1124.2
025200 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       IX1124.2
025300 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       IX1124.2
025400 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       IX1124.2
025500 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       IX1124.2
025600 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       IX1124.2
025700 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      IX1124.2
025800 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       IX1124.2
025900 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     IX1124.2
026000 01  CCVS-H-1.                                                    IX1124.2
026100     02  FILLER                    PIC X(39)    VALUE SPACES.     IX1124.2
026200     02  FILLER                    PIC X(42)    VALUE             IX1124.2
026300     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 IX1124.2
026400     02  FILLER                    PIC X(39)    VALUE SPACES.     IX1124.2
026500 01  CCVS-H-2A.                                                   IX1124.2
026600   02  FILLER                        PIC X(40)  VALUE SPACE.      IX1124.2
026700   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  IX1124.2
026800   02  FILLER                        PIC XXXX   VALUE             IX1124.2
026900     "4.2 ".                                                      IX1124.2
027000   02  FILLER                        PIC X(28)  VALUE             IX1124.2
027100            " COPY - NOT FOR DISTRIBUTION".                       IX1124.2
027200   02  FILLER                        PIC X(41)  VALUE SPACE.      IX1124.2
027300                                                                  IX1124.2
027400 01  CCVS-H-2B.                                                   IX1124.2
027500   02  FILLER                        PIC X(15)  VALUE             IX1124.2
027600            "TEST RESULT OF ".                                    IX1124.2
027700   02  TEST-ID                       PIC X(9).                    IX1124.2
027800   02  FILLER                        PIC X(4)   VALUE             IX1124.2
027900            " IN ".                                               IX1124.2
028000   02  FILLER                        PIC X(12)  VALUE             IX1124.2
028100     " HIGH       ".                                              IX1124.2
028200   02  FILLER                        PIC X(22)  VALUE             IX1124.2
028300            " LEVEL VALIDATION FOR ".                             IX1124.2
028400   02  FILLER                        PIC X(58)  VALUE             IX1124.2
028500     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IX1124.2
028600 01  CCVS-H-3.                                                    IX1124.2
028700     02  FILLER                      PIC X(34)  VALUE             IX1124.2
028800            " FOR OFFICIAL USE ONLY    ".                         IX1124.2
028900     02  FILLER                      PIC X(58)  VALUE             IX1124.2
029000     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".IX1124.2
029100     02  FILLER                      PIC X(28)  VALUE             IX1124.2
029200            "  COPYRIGHT   1985 ".                                IX1124.2
029300 01  CCVS-E-1.                                                    IX1124.2
029400     02 FILLER                       PIC X(52)  VALUE SPACE.      IX1124.2
029500     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              IX1124.2
029600     02 ID-AGAIN                     PIC X(9).                    IX1124.2
029700     02 FILLER                       PIC X(45)  VALUE SPACES.     IX1124.2
029800 01  CCVS-E-2.                                                    IX1124.2
029900     02  FILLER                      PIC X(31)  VALUE SPACE.      IX1124.2
030000     02  FILLER                      PIC X(21)  VALUE SPACE.      IX1124.2
030100     02 CCVS-E-2-2.                                               IX1124.2
030200         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      IX1124.2
030300         03 FILLER                   PIC X      VALUE SPACE.      IX1124.2
030400         03 ENDER-DESC               PIC X(44)  VALUE             IX1124.2
030500            "ERRORS ENCOUNTERED".                                 IX1124.2
030600 01  CCVS-E-3.                                                    IX1124.2
030700     02  FILLER                      PIC X(22)  VALUE             IX1124.2
030800            " FOR OFFICIAL USE ONLY".                             IX1124.2
030900     02  FILLER                      PIC X(12)  VALUE SPACE.      IX1124.2
031000     02  FILLER                      PIC X(58)  VALUE             IX1124.2
031100     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IX1124.2
031200     02  FILLER                      PIC X(13)  VALUE SPACE.      IX1124.2
031300     02 FILLER                       PIC X(15)  VALUE             IX1124.2
031400             " COPYRIGHT 1985".                                   IX1124.2
031500 01  CCVS-E-4.                                                    IX1124.2
031600     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      IX1124.2
031700     02 FILLER                       PIC X(4)   VALUE " OF ".     IX1124.2
031800     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      IX1124.2
031900     02 FILLER                       PIC X(40)  VALUE             IX1124.2
032000      "  TESTS WERE EXECUTED SUCCESSFULLY".                       IX1124.2
032100 01  XXINFO.                                                      IX1124.2
032200     02 FILLER                       PIC X(19)  VALUE             IX1124.2
032300            "*** INFORMATION ***".                                IX1124.2
032400     02 INFO-TEXT.                                                IX1124.2
032500       04 FILLER                     PIC X(8)   VALUE SPACE.      IX1124.2
032600       04 XXCOMPUTED                 PIC X(20).                   IX1124.2
032700       04 FILLER                     PIC X(5)   VALUE SPACE.      IX1124.2
032800       04 XXCORRECT                  PIC X(20).                   IX1124.2
032900     02 INF-ANSI-REFERENCE           PIC X(48).                   IX1124.2
033000 01  HYPHEN-LINE.                                                 IX1124.2
033100     02 FILLER  PIC IS X VALUE IS SPACE.                          IX1124.2
033200     02 FILLER  PIC IS X(65)    VALUE IS "************************IX1124.2
033300-    "*****************************************".                 IX1124.2
033400     02 FILLER  PIC IS X(54)    VALUE IS "************************IX1124.2
033500-    "******************************".                            IX1124.2
033600 01  CCVS-PGM-ID                     PIC X(9)   VALUE             IX1124.2
033700     "IX112A".                                                    IX1124.2
033800 01 TEST-NUMBER                      PIC 9 VALUE ZERO.            IX1124.2
033900                                                                  IX1124.2
034000 PROCEDURE DIVISION.                                              IX1124.2
034100 DECLARATIVES.                                                    IX1124.2
034200                                                                  IX1124.2
034300 SECT-IX105-0002 SECTION.                                         IX1124.2
034400     USE AFTER EXCEPTION PROCEDURE ON IX-VS2.                     IX1124.2
034500 INPUT-PROCESS.                                                   IX1124.2
034600         MOVE 1 TO PERM-ERRORS.                                   IX1124.2
034700         IF TEST-NUMBER NOT = 7 GO TO END-DECL.                   IX1124.2
034800 D-RWR-TEST-GF-01-1.                                              IX1124.2
034900     IF IX-VS2-STATUS  = "00"                                     IX1124.2
035000         GO TO D-RWR-PASS-GF-01-0.                                IX1124.2
035100     IF IX-VS2-STATUS  = "44"                                     IX1124.2
035200         GO TO D-RWR-PASS-GF-01-0.                                IX1124.2
035300 D-RWR-FAIL-GF-01-0.                                              IX1124.2
035400     MOVE "IX-5, 1.3.4, (5) d 1 & 2;  SHORT RECORD" TO RE-MARK.   IX1124.2
035500     PERFORM D-FAIL.                                              IX1124.2
035600     MOVE  IX-VS2-STATUS      TO COMPUTED-A.                      IX1124.2
035700     MOVE "00 OR 44" TO CORRECT-X.                                IX1124.2
035800     GO TO D-RWR-WRITE-GF-01-0.                                   IX1124.2
035900 D-RWR-PASS-GF-01-0.                                              IX1124.2
036000     PERFORM D-PASS.                                              IX1124.2
036100 D-RWR-WRITE-GF-01-0.                                             IX1124.2
036200     PERFORM D-PRINT-DETAIL.                                      IX1124.2
036300 D-CLOSE-FILES.                                                   IX1124.2
036400*    OPEN I-O RAW-DATA.                                           IX1124.2
036500*    MOVE CCVS-PGM-ID TO RAW-DATA-KEY.                            IX1124.2
036600*    READ RAW-DATA INVALID KEY GO TO D-END-E-2.                   IX1124.2
036700*    MOVE "OK.     " TO C-ABORT.                                  IX1124.2
036800*    MOVE PASS-COUNTER TO C-OK.                                   IX1124.2
036900*    MOVE ERROR-HOLD   TO C-ALL.                                  IX1124.2
037000*    MOVE ERROR-COUNTER TO C-FAIL.                                IX1124.2
037100*    MOVE DELETE-COUNTER TO C-DELETED.                            IX1124.2
037200*    MOVE INSPECT-COUNTER TO C-INSPECT.                           IX1124.2
037300*    REWRITE RAW-DATA-SATZ INVALID KEY GO TO D-END-E-2.           IX1124.2
037400*D-END-E-2.                                                       IX1124.2
037500*    CLOSE RAW-DATA.                                              IX1124.2
037600     PERFORM D-END-ROUTINE THRU D-END-ROUTINE-13.                 IX1124.2
037700     CLOSE PRINT-FILE.                                            IX1124.2
037800 D-TERMINATE-CCVS.                                                IX1124.2
037900*    EXIT PROGRAM.                                                IX1124.2
038000*D-TERMINATE-CALL.                                                IX1124.2
038100     STOP     RUN.                                                IX1124.2
038200 D-PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.         IX1124.2
038300 D-FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.        IX1124.2
038400 D-PRINT-DETAIL.                                                  IX1124.2
038500     IF REC-CT NOT EQUAL TO ZERO                                  IX1124.2
038600             MOVE "." TO PARDOT-X                                 IX1124.2
038700             MOVE REC-CT TO DOTVALUE.                             IX1124.2
038800     MOVE TEST-RESULTS TO PRINT-REC. PERFORM D-WRITE-LINE.        IX1124.2
038900     IF P-OR-F EQUAL TO "FAIL*"  PERFORM D-WRITE-LINE             IX1124.2
039000        PERFORM D-FAIL-ROUTINE THRU D-FAIL-ROUTINE-EX             IX1124.2
039100     ELSE PERFORM D-BAIL-OUT THRU D-BAIL-OUT-EX.                  IX1124.2
039200     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              IX1124.2
039300     MOVE SPACE TO CORRECT-X.                                     IX1124.2
039400     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         IX1124.2
039500     MOVE     SPACE TO RE-MARK.                                   IX1124.2
039600 D-END-ROUTINE.                                                   IX1124.2
039700     MOVE HYPHEN-LINE TO DUMMY-RECORD.                            IX1124.2
039800     PERFORM D-WRITE-LINE 5 TIMES.                                IX1124.2
039900 D-END-RTN-EXIT.                                                  IX1124.2
040000     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM D-WRITE-LINE 2 TIMES. IX1124.2
040100 D-END-ROUTINE-1.                                                 IX1124.2
040200      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      IX1124.2
040300      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               IX1124.2
040400      ADD PASS-COUNTER TO ERROR-HOLD.                             IX1124.2
040500      MOVE PASS-COUNTER TO CCVS-E-4-1.                            IX1124.2
040600      MOVE ERROR-HOLD TO CCVS-E-4-2.                              IX1124.2
040700      MOVE CCVS-E-4 TO CCVS-E-2-2.                                IX1124.2
040800      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM D-WRITE-LINE.         IX1124.2
040900 D-END-ROUTINE-12.                                                IX1124.2
041000      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        IX1124.2
041100     IF       ERROR-COUNTER IS EQUAL TO ZERO                      IX1124.2
041200         MOVE "NO " TO ERROR-TOTAL                                IX1124.2
041300         ELSE                                                     IX1124.2
041400         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       IX1124.2
041500     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           IX1124.2
041600     PERFORM D-WRITE-LINE.                                        IX1124.2
041700 D-END-ROUTINE-13.                                                IX1124.2
041800     IF DELETE-COUNTER IS EQUAL TO ZERO                           IX1124.2
041900         MOVE "NO " TO ERROR-TOTAL  ELSE                          IX1124.2
042000         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      IX1124.2
042100     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   IX1124.2
042200     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM D-WRITE-LINE.         IX1124.2
042300      IF   INSPECT-COUNTER EQUAL TO ZERO                          IX1124.2
042400          MOVE "NO " TO ERROR-TOTAL                               IX1124.2
042500      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   IX1124.2
042600      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            IX1124.2
042700     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM D-WRITE-LINE.         IX1124.2
042800     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM D-WRITE-LINE.         IX1124.2
042900 D-WRITE-LINE.                                                    IX1124.2
043000     ADD 1 TO RECORD-COUNT.                                       IX1124.2
043100*    IF RECORD-COUNT GREATER 42                                   IX1124.2
043200*      MOVE DUMMY-RECORD TO DUMMY-HOLD                            IX1124.2
043300*      MOVE SPACE TO DUMMY-RECORD                                 IX1124.2
043400*      WRITE DUMMY-RECORD AFTER ADVANCING PAGE                    IX1124.2
043500*      MOVE CCVS-H-1  TO DUMMY-RECORD PERFORM D-WRT-LN 2 TIMES    IX1124.2
043600*      MOVE CCVS-H-2A TO DUMMY-RECORD PERFORM D-WRT-LN 2 TIMES    IX1124.2
043700*      MOVE CCVS-H-2B TO DUMMY-RECORD PERFORM D-WRT-LN 3 TIMES    IX1124.2
043800*      MOVE CCVS-H-3  TO DUMMY-RECORD PERFORM D-WRT-LN 3 TIMES    IX1124.2
043900*      MOVE CCVS-C-1  TO DUMMY-RECORD PERFORM D-WRT-LN            IX1124.2
044000*      MOVE CCVS-C-2  TO DUMMY-RECORD PERFORM D-WRT-LN            IX1124.2
044100*      MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM D-WRT-LN          IX1124.2
044200*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          IX1124.2
044300*        MOVE ZERO TO RECORD-COUNT.                               IX1124.2
044400     PERFORM D-WRT-LN.                                            IX1124.2
044500 D-WRT-LN.                                                        IX1124.2
044600     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               IX1124.2
044700     MOVE SPACE TO DUMMY-RECORD.                                  IX1124.2
044800 D-BLANK-LINE-PRINT.                                              IX1124.2
044900     PERFORM D-WRT-LN.                                            IX1124.2
045000 D-FAIL-ROUTINE.                                                  IX1124.2
045100     IF     COMPUTED-X NOT EQUAL TO SPACE                         IX1124.2
045200            GO TO  D-FAIL-ROUTINE-WRITE.                          IX1124.2
045300     IF  CORRECT-X NOT EQUAL TO SPACE GO TO D-FAIL-ROUTINE-WRITE. IX1124.2
045400     MOVE ANSI-REFERENCE TO INF-ANSI-REFERENCE.                   IX1124.2
045500     MOVE "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.    IX1124.2
045600     MOVE  XXINFO TO DUMMY-RECORD. PERFORM D-WRITE-LINE 2 TIMES.  IX1124.2
045700     MOVE  SPACES TO INF-ANSI-REFERENCE.                          IX1124.2
045800     GO TO D-FAIL-ROUTINE-EX.                                     IX1124.2
045900 D-FAIL-ROUTINE-WRITE.                                            IX1124.2
046000     MOVE  TEST-COMPUTED TO PRINT-REC PERFORM D-WRITE-LINE        IX1124.2
046100     MOVE  ANSI-REFERENCE TO COR-ANSI-REFERENCE.                  IX1124.2
046200     MOVE TEST-CORRECT TO PRINT-REC PERFORM D-WRITE-LINE 2 TIMES. IX1124.2
046300     MOVE  SPACES TO COR-ANSI-REFERENCE.                          IX1124.2
046400 D-FAIL-ROUTINE-EX. EXIT.                                         IX1124.2
046500 D-BAIL-OUT.                                                      IX1124.2
046600     IF  COMPUTED-A NOT EQUAL TO SPACE GO TO D-BAIL-OUT-WRITE.    IX1124.2
046700     IF  CORRECT-A EQUAL TO SPACE GO TO D-BAIL-OUT-EX.            IX1124.2
046800 D-BAIL-OUT-WRITE.                                                IX1124.2
046900     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  IX1124.2
047000     MOVE  ANSI-REFERENCE TO INF-ANSI-REFERENCE.                  IX1124.2
047100     MOVE  XXINFO TO DUMMY-RECORD. PERFORM D-WRITE-LINE 2 TIMES.  IX1124.2
047200     MOVE  SPACES TO INF-ANSI-REFERENCE.                          IX1124.2
047300 D-BAIL-OUT-EX. EXIT.                                             IX1124.2
047400                                                                  IX1124.2
047500 END-DECL.                                                        IX1124.2
047600 END DECLARATIVES.                                                IX1124.2
047700                                                                  IX1124.2
047800                                                                  IX1124.2
047900 CCVS1 SECTION.                                                   IX1124.2
048000 OPEN-FILES.                                                      IX1124.2
048100*    OPEN I-O RAW-DATA.                                           IX1124.2
048200*    MOVE CCVS-PGM-ID TO RAW-DATA-KEY.                            IX1124.2
048300*    READ RAW-DATA INVALID KEY GO TO END-E-1.                     IX1124.2
048400*    MOVE "ABORTED " TO C-ABORT.                                  IX1124.2
048500*    ADD 1 TO C-NO-OF-TESTS.                                      IX1124.2
048600*    ACCEPT C-DATE  FROM DATE.                                    IX1124.2
048700*    ACCEPT C-TIME  FROM TIME.                                    IX1124.2
048800*    REWRITE RAW-DATA-SATZ INVALID KEY GO TO END-E-1.             IX1124.2
048900*END-E-1.                                                         IX1124.2
049000*    CLOSE RAW-DATA.                                              IX1124.2
049100     OPEN    OUTPUT PRINT-FILE.                                   IX1124.2
049200     MOVE  CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.  IX1124.2
049300     MOVE    SPACE TO TEST-RESULTS.                               IX1124.2
049400     PERFORM HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.              IX1124.2
049500     MOVE    ZERO TO REC-SKL-SUB.                                 IX1124.2
049600     PERFORM CCVS-INIT-FILE 9 TIMES.                              IX1124.2
049700 CCVS-INIT-FILE.                                                  IX1124.2
049800     ADD     1 TO REC-SKL-SUB.                                    IX1124.2
049900     MOVE    FILE-RECORD-INFO-SKELETON                            IX1124.2
050000          TO FILE-RECORD-INFO (REC-SKL-SUB).                      IX1124.2
050100 CCVS-INIT-EXIT.                                                  IX1124.2
050200     GO TO CCVS1-EXIT.                                            IX1124.2
050300 CLOSE-FILES.                                                     IX1124.2
050400*    OPEN I-O RAW-DATA.                                           IX1124.2
050500*    MOVE CCVS-PGM-ID TO RAW-DATA-KEY.                            IX1124.2
050600*    READ RAW-DATA INVALID KEY GO TO END-E-2.                     IX1124.2
050700*    MOVE "OK.     " TO C-ABORT.                                  IX1124.2
050800*    MOVE PASS-COUNTER TO C-OK.                                   IX1124.2
050900*    MOVE ERROR-HOLD   TO C-ALL.                                  IX1124.2
051000*    MOVE ERROR-COUNTER TO C-FAIL.                                IX1124.2
051100*    MOVE DELETE-COUNTER TO C-DELETED.                            IX1124.2
051200*    MOVE INSPECT-COUNTER TO C-INSPECT.                           IX1124.2
051300*    REWRITE RAW-DATA-SATZ INVALID KEY GO TO END-E-2.             IX1124.2
051400*END-E-2.                                                         IX1124.2
051500*    CLOSE RAW-DATA.                                              IX1124.2
051600     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   IX1124.2
051700 TERMINATE-CCVS.                                                  IX1124.2
051800*    EXIT PROGRAM.                                                IX1124.2
051900*TERMINATE-CALL.                                                  IX1124.2
052000     STOP     RUN.                                                IX1124.2
052100 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         IX1124.2
052200 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           IX1124.2
052300 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          IX1124.2
052400 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      IX1124.2
052500     MOVE "****TEST DELETED****" TO RE-MARK.                      IX1124.2
052600 PRINT-DETAIL.                                                    IX1124.2
052700     IF REC-CT NOT EQUAL TO ZERO                                  IX1124.2
052800             MOVE "." TO PARDOT-X                                 IX1124.2
052900             MOVE REC-CT TO DOTVALUE.                             IX1124.2
053000     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      IX1124.2
053100     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               IX1124.2
053200        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 IX1124.2
053300          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 IX1124.2
053400     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              IX1124.2
053500     MOVE SPACE TO CORRECT-X.                                     IX1124.2
053600     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         IX1124.2
053700     MOVE     SPACE TO RE-MARK.                                   IX1124.2
053800 HEAD-ROUTINE.                                                    IX1124.2
053900     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  IX1124.2
054000     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  IX1124.2
054100     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  IX1124.2
054200     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  IX1124.2
054300 COLUMN-NAMES-ROUTINE.                                            IX1124.2
054400     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IX1124.2
054500     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IX1124.2
054600     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        IX1124.2
054700 END-ROUTINE.                                                     IX1124.2
054800     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.IX1124.2
054900 END-RTN-EXIT.                                                    IX1124.2
055000     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IX1124.2
055100 END-ROUTINE-1.                                                   IX1124.2
055200      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      IX1124.2
055300      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               IX1124.2
055400      ADD PASS-COUNTER TO ERROR-HOLD.                             IX1124.2
055500*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   IX1124.2
055600      MOVE PASS-COUNTER TO CCVS-E-4-1.                            IX1124.2
055700      MOVE ERROR-HOLD TO CCVS-E-4-2.                              IX1124.2
055800      MOVE CCVS-E-4 TO CCVS-E-2-2.                                IX1124.2
055900      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           IX1124.2
056000  END-ROUTINE-12.                                                 IX1124.2
056100      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        IX1124.2
056200     IF       ERROR-COUNTER IS EQUAL TO ZERO                      IX1124.2
056300         MOVE "NO " TO ERROR-TOTAL                                IX1124.2
056400         ELSE                                                     IX1124.2
056500         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       IX1124.2
056600     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           IX1124.2
056700     PERFORM WRITE-LINE.                                          IX1124.2
056800 END-ROUTINE-13.                                                  IX1124.2
056900     IF DELETE-COUNTER IS EQUAL TO ZERO                           IX1124.2
057000         MOVE "NO " TO ERROR-TOTAL  ELSE                          IX1124.2
057100         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      IX1124.2
057200     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   IX1124.2
057300     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IX1124.2
057400      IF   INSPECT-COUNTER EQUAL TO ZERO                          IX1124.2
057500          MOVE "NO " TO ERROR-TOTAL                               IX1124.2
057600      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   IX1124.2
057700      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            IX1124.2
057800      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          IX1124.2
057900     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IX1124.2
058000 WRITE-LINE.                                                      IX1124.2
058100     ADD 1 TO RECORD-COUNT.                                       IX1124.2
058200*    IF RECORD-COUNT GREATER 42                                   IX1124.2
058300*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          IX1124.2
058400*        MOVE SPACE TO DUMMY-RECORD                               IX1124.2
058500*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  IX1124.2
058600*        MOVE CCVS-H-1  TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES    IX1124.2
058700*        MOVE CCVS-H-2A TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES    IX1124.2
058800*        MOVE CCVS-H-2B TO DUMMY-RECORD PERFORM WRT-LN 3 TIMES    IX1124.2
058900*        MOVE CCVS-H-3  TO DUMMY-RECORD PERFORM WRT-LN 3 TIMES    IX1124.2
059000*        MOVE CCVS-C-1  TO DUMMY-RECORD PERFORM WRT-LN            IX1124.2
059100*        MOVE CCVS-C-2  TO DUMMY-RECORD PERFORM WRT-LN            IX1124.2
059200*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          IX1124.2
059300*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          IX1124.2
059400*        MOVE ZERO TO RECORD-COUNT.                               IX1124.2
059500     PERFORM WRT-LN.                                              IX1124.2
059600 WRT-LN.                                                          IX1124.2
059700     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               IX1124.2
059800     MOVE SPACE TO DUMMY-RECORD.                                  IX1124.2
059900 BLANK-LINE-PRINT.                                                IX1124.2
060000     PERFORM WRT-LN.                                              IX1124.2
060100 FAIL-ROUTINE.                                                    IX1124.2
060200     IF     COMPUTED-X NOT EQUAL TO SPACE                         IX1124.2
060300            GO TO   FAIL-ROUTINE-WRITE.                           IX1124.2
060400     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.IX1124.2
060500     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 IX1124.2
060600     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   IX1124.2
060700     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IX1124.2
060800     MOVE   SPACES TO INF-ANSI-REFERENCE.                         IX1124.2
060900     GO TO  FAIL-ROUTINE-EX.                                      IX1124.2
061000 FAIL-ROUTINE-WRITE.                                              IX1124.2
061100     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         IX1124.2
061200     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 IX1124.2
061300     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. IX1124.2
061400     MOVE   SPACES TO COR-ANSI-REFERENCE.                         IX1124.2
061500 FAIL-ROUTINE-EX. EXIT.                                           IX1124.2
061600 BAIL-OUT.                                                        IX1124.2
061700     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   IX1124.2
061800     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           IX1124.2
061900 BAIL-OUT-WRITE.                                                  IX1124.2
062000     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  IX1124.2
062100     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 IX1124.2
062200     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IX1124.2
062300     MOVE   SPACES TO INF-ANSI-REFERENCE.                         IX1124.2
062400 BAIL-OUT-EX. EXIT.                                               IX1124.2
062500 CCVS1-EXIT.                                                      IX1124.2
062600     EXIT.                                                        IX1124.2
062700                                                                  IX1124.2
062800 SECT-IX112A-0003 SECTION.                                        IX1124.2
062900 SEQ-INIT-010.                                                    IX1124.2
063000     MOVE "IX-VS2" TO XFILE-NAME (1).                             IX1124.2
063100     MOVE "R1-F-G" TO XRECORD-NAME (1).                           IX1124.2
063200     MOVE CCVS-PGM-ID TO XPROGRAM-NAME (1).                       IX1124.2
063300     MOVE 000240 TO XRECORD-LENGTH (1).                           IX1124.2
063400     MOVE "RC" TO CHARS-OR-RECORDS (1).                           IX1124.2
063500     MOVE 0002 TO XBLOCK-SIZE (1).                                IX1124.2
063600     MOVE 000050 TO RECORDS-IN-FILE (1).                          IX1124.2
063700     MOVE "IX" TO XFILE-ORGANIZATION (1).                         IX1124.2
063800     MOVE "S" TO XLABEL-TYPE (1).                                 IX1124.2
063900     MOVE 000001 TO XRECORD-NUMBER (1).                           IX1124.2
064000     MOVE 0 TO COUNT-OF-RECS.                                     IX1124.2
064100                                                                  IX1124.2
064200******************************************************************IX1124.2
064300*   TEST  1                                                      *IX1124.2
064400*         OPEN OUTPUT ...                 00 EXPECTED            *IX1124.2
064500*         IX-3, 1.3.4 (1) a                                      *IX1124.2
064600*    STATUS 00 CHECK ON OUTPUT FILE IX-VS2                       *IX1124.2
064700*    THE OUTPUT STATEMENT IS SUCCESSFULLY EXECUTED               *IX1124.2
064800******************************************************************IX1124.2
064900 OPN-INIT-GF-01-0.                                                IX1124.2
065000     ADD  1 TO TEST-NUMBER.                                       IX1124.2
065100     MOVE 1 TO STATUS-TEST-00.                                    IX1124.2
065200     MOVE SPACES TO IX-VS2-STATUS.                                IX1124.2
065300     MOVE "OPEN OUTPUT: 00 EXP." TO FEATURE.                      IX1124.2
065400     MOVE "OPN-TEST-GF-01-0" TO PAR-NAME.                         IX1124.2
065500     OPEN                                                         IX1124.2
065600        OUTPUT IX-VS2.                                            IX1124.2
065700     IF IX-VS2-STATUS EQUAL TO "00"                               IX1124.2
065800         GO TO OPN-PASS-GF-01-0.                                  IX1124.2
065900 OPN-FAIL-GF-01-0.                                                IX1124.2
066000     MOVE "IX-3, 1.3.4, (1) a. " TO RE-MARK.                      IX1124.2
066100     PERFORM FAIL.                                                IX1124.2
066200     MOVE IX-VS2-STATUS TO COMPUTED-A.                            IX1124.2
066300     MOVE "00" TO CORRECT-X.                                      IX1124.2
066400     GO TO OPN-WRITE-GF-01-0.                                     IX1124.2
066500 OPN-PASS-GF-01-0.                                                IX1124.2
066600     PERFORM PASS.                                                IX1124.2
066700 OPN-WRITE-GF-01-0.                                               IX1124.2
066800     PERFORM PRINT-DETAIL.                                        IX1124.2
066900******************************************************************IX1124.2
067000*   TEST  2                                                      *IX1124.2
067100*         WRITE                           00 EXPECTED            *IX1124.2
067200*         IX-3, 1.3.4 (1) a                                      *IX1124.2
067300*    CREATING A INDEXED FILE WITH 50 RECORDS                     *IX1124.2
067400*    KEY:  FROM  000000001 TO 000000050                          *IX1124.2
067500******************************************************************IX1124.2
067600 WRI-INIT-GF-01-0.                                                IX1124.2
067700     ADD  1 TO TEST-NUMBER.                                       IX1124.2
067800     MOVE SPACES TO IX-VS2-STATUS.                                IX1124.2
067900     MOVE 0 TO STATUS-TEST-00.                                    IX1124.2
068000     MOVE "WRITE: 00 EXPECTED" TO FEATURE.                        IX1124.2
068100     MOVE "WRI-TEST-GF-01-0" TO PAR-NAME.                         IX1124.2
068200 WRI-TEST-GF-01-0.                                                IX1124.2
068300     MOVE XRECORD-NUMBER (1) TO GRP-0101-KEY, COUNT-OF-RECS.      IX1124.2
068400     MOVE GRP-0101 TO XRECORD-KEY (1).                            IX1124.2
068500     MOVE GRP-0102 TO ALTERNATE-KEY1 (1).                         IX1124.2
068600*    THE VALUE OF THE ALTERNATE KEY IS 50 TIMES UNCHANGED        *IX1124.2
068700     MOVE FILE-RECORD-INFO (1) TO IX-VS2R1-F-G-240.               IX1124.2
068800     WRITE IX-VS2R1-F-G-240.                                      IX1124.2
068900     IF IX-VS2-STATUS  NOT = "00"                                 IX1124.2
069000         MOVE 1 TO STATUS-TEST-00                                 IX1124.2
069100         GO TO WRI-FAIL-GF-01-0.                                  IX1124.2
069200     IF XRECORD-NUMBER (1) EQUAL TO 50                            IX1124.2
069300         GO TO WRI-TEST-GF-01-1.                                  IX1124.2
069400     ADD 1 TO XRECORD-NUMBER (1).                                 IX1124.2
069500     GO TO WRI-TEST-GF-01-0.                                      IX1124.2
069600 WRI-TEST-GF-01-1.                                                IX1124.2
069700     IF RECORDS-IN-ERROR EQUAL TO ZERO                            IX1124.2
069800         GO TO WRI-PASS-GF-01-0.                                  IX1124.2
069900     MOVE "ERROR IN CREATING FILE" TO RE-MARK.                    IX1124.2
070000 WRI-FAIL-GF-01-0.                                                IX1124.2
070100     MOVE "IX-3, 1.3.4, (1) a. " TO RE-MARK.                      IX1124.2
070200     PERFORM FAIL.                                                IX1124.2
070300     MOVE "RECORDS WRITTEN =" TO COMPUTED-A.                      IX1124.2
070400     GO TO WRI-WRITE-GF-01-0.                                     IX1124.2
070500 WRI-PASS-GF-01-0.                                                IX1124.2
070600     PERFORM PASS.                                                IX1124.2
070700 WRI-WRITE-GF-01-0.                                               IX1124.2
070800     PERFORM PRINT-DETAIL.                                        IX1124.2
070900     MOVE "FILE CREATED, RECS =" TO COMPUTED-A.                   IX1124.2
071000     MOVE "CREATE FILE IX-VS2" TO FEATURE.                        IX1124.2
071100     MOVE "WRI-TEST-GF-01-1" TO PAR-NAME.                         IX1124.2
071200     MOVE COUNT-OF-RECS TO CORRECT-18V0.                          IX1124.2
071300     PERFORM PRINT-DETAIL.                                        IX1124.2
071400                                                                  IX1124.2
071500******************************************************************IX1124.2
071600*   TEST  3                                                      *IX1124.2
071700*         WRITE  (WRONG SEQUENCE)         21 EXPECTED            *IX1124.2
071800*         IX-4, 1.3.4 (3) a                                      *IX1124.2
071900*    KEY:        000000049                                       *IX1124.2
072000******************************************************************IX1124.2
072100 WRI-INIT-GF-02-0.                                                IX1124.2
072200     ADD  1 TO TEST-NUMBER.                                       IX1124.2
072300     MOVE SPACES TO IX-VS2-STATUS.                                IX1124.2
072400     MOVE 0 TO STATUS-TEST-00.                                    IX1124.2
072500     MOVE "WRITE:WRONG SEQ 21EX" TO FEATURE.                      IX1124.2
072600     MOVE "WRI-TEST-GF-02-0" TO PAR-NAME.                         IX1124.2
072700     MOVE 49 TO XRECORD-NUMBER (1).                               IX1124.2
072800 WRI-TEST-GF-02-0.                                                IX1124.2
072900     MOVE XRECORD-NUMBER (1) TO GRP-0101-KEY, COUNT-OF-RECS.      IX1124.2
073000     MOVE GRP-0101 TO XRECORD-KEY (1).                            IX1124.2
073100     MOVE GRP-0102 TO ALTERNATE-KEY1 (1).                         IX1124.2
073200     MOVE FILE-RECORD-INFO (1) TO IX-VS2R1-F-G-240.               IX1124.2
073300     WRITE IX-VS2R1-F-G-240 INVALID KEY GO TO WRI-TEST-GF-02-1.   IX1124.2
073400 WRI-TEST-GF-02-1.                                                IX1124.2
073500     IF IX-VS2-STATUS  = "21"                                     IX1124.2
073600         GO TO WRI-PASS-GF-02-0.                                  IX1124.2
073700 WRI-FAIL-GF-02-0.                                                IX1124.2
073800     MOVE "IX-4, 1.3.4, (3) a. " TO RE-MARK.                      IX1124.2
073900     PERFORM FAIL.                                                IX1124.2
074000     MOVE  IX-VS2-STATUS      TO COMPUTED-A.                      IX1124.2
074100     MOVE "21" TO CORRECT-X.                                      IX1124.2
074200     GO TO WRI-WRITE-GF-02-0.                                     IX1124.2
074300 WRI-PASS-GF-02-0.                                                IX1124.2
074400     PERFORM PASS.                                                IX1124.2
074500 WRI-WRITE-GF-02-0.                                               IX1124.2
074600     PERFORM PRINT-DETAIL.                                        IX1124.2
074700                                                                  IX1124.2
074800******************************************************************IX1124.2
074900*   TEST  4                                                      *IX1124.2
075000*         CLOSE OUTPUT                    00 EXPECTED            *IX1124.2
075100*         IX-3, 1.3.4 (1) a                                      *IX1124.2
075200******************************************************************IX1124.2
075300 CLO-INIT-GF-01-0.                                                IX1124.2
075400     ADD  1 TO TEST-NUMBER.                                       IX1124.2
075500     MOVE SPACES TO IX-VS2-STATUS.                                IX1124.2
075600     MOVE "CLOSE OUTPUT:00 EXP." TO FEATURE.                      IX1124.2
075700     MOVE "CLO-TEST-GF-01-0" TO PAR-NAME.                         IX1124.2
075800 CLO-TEST-GF-01-0.                                                IX1124.2
075900     CLOSE IX-VS2.                                                IX1124.2
076000     IF IX-VS2-STATUS = "00"                                      IX1124.2
076100         GO TO CLO-PASS-GF-01-0.                                  IX1124.2
076200 CLO-FAIL-GF-01-0.                                                IX1124.2
076300     MOVE "IX-3, 1.3.4, (1) a. " TO RE-MARK.                      IX1124.2
076400     PERFORM FAIL.                                                IX1124.2
076500     MOVE IX-VS2-STATUS TO COMPUTED-A.                            IX1124.2
076600     MOVE "00" TO CORRECT-X.                                      IX1124.2
076700     GO TO CLO-WRITE-GF-01-0.                                     IX1124.2
076800 CLO-PASS-GF-01-0.                                                IX1124.2
076900     PERFORM PASS.                                                IX1124.2
077000 CLO-WRITE-GF-01-0.                                               IX1124.2
077100     PERFORM PRINT-DETAIL.                                        IX1124.2
077200                                                                  IX1124.2
077300******************************************************************IX1124.2
077400*    A INDEXED FILE WITH 50 RECORDS HAS BEEN CREATED.            *IX1124.2
077500******************************************************************IX1124.2
077600                                                                  IX1124.2
077700******************************************************************IX1124.2
077800*   TEST  5                                                      *IX1124.2
077900*         OPEN INPUT                      00 EXPECTED            *IX1124.2
078000*         IX-3, 1.3.4 (1) a                                      *IX1124.2
078100******************************************************************IX1124.2
078200 OPN-INIT-GF-02-0.                                                IX1124.2
078300     ADD  1 TO TEST-NUMBER.                                       IX1124.2
078400     MOVE SPACES TO IX-VS2-STATUS.                                IX1124.2
078500     MOVE "OPEN INPUT:  00 EXP." TO FEATURE.                      IX1124.2
078600     MOVE "OPN-TEST-GF-02-0" TO PAR-NAME.                         IX1124.2
078700 OPN-TEST-GF-02-0.                                                IX1124.2
078800     OPEN                                                         IX1124.2
078900        INPUT IX-VS2.                                             IX1124.2
079000     IF IX-VS2-STATUS EQUAL TO "00"                               IX1124.2
079100         GO TO OPN-PASS-GF-02-0.                                  IX1124.2
079200 OPN-FAIL-GF-02-0.                                                IX1124.2
079300     MOVE "IX-3, 1.3.4, (1) a." TO RE-MARK.                       IX1124.2
079400     PERFORM FAIL.                                                IX1124.2
079500     MOVE IX-VS2-STATUS TO COMPUTED-A.                            IX1124.2
079600     MOVE "00" TO CORRECT-X.                                      IX1124.2
079700     GO TO OPN-WRITE-GF-02-0.                                     IX1124.2
079800 OPN-PASS-GF-02-0.                                                IX1124.2
079900     PERFORM PASS.                                                IX1124.2
080000 OPN-WRITE-GF-02-0.                                               IX1124.2
080100     PERFORM PRINT-DETAIL.                                        IX1124.2
080200                                                                  IX1124.2
080300******************************************************************IX1124.2
080400*   TEST  6                                                      *IX1124.2
080500*         READ                  OO EXPECTED                      *IX1124.2
080600*         IX-3, 1.3.4 (1) a                                      *IX1124.2
080700******************************************************************IX1124.2
080800 REA-INIT-F1-01-0.                                                IX1124.2
080900     ADD  1 TO TEST-NUMBER.                                       IX1124.2
081000     MOVE 1 TO STATUS-TEST-10.                                    IX1124.2
081100     MOVE SPACES TO IX-VS2-STATUS.                                IX1124.2
081200     MOVE ZERO TO COUNT-OF-RECS.                                  IX1124.2
081300     MOVE ZERO TO PERM-ERRORS.                                    IX1124.2
081400     MOVE ZERO TO EOF-FLAG.                                       IX1124.2
081500 REA-TEST-F1-01-0.                                                IX1124.2
081600     READ IX-VS2.                                                 IX1124.2
081700     MOVE "REA-TEST-F1-01-0" TO PAR-NAME.                         IX1124.2
081800     MOVE "READ (USE):  00 EXP."     TO FEATURE.                  IX1124.2
081900     IF IX-VS2-STATUS = "00"                                      IX1124.2
082000         GO TO REA-PASS-F1-01-0.                                  IX1124.2
082100 REA-FAIL-F1-01-0.                                                IX1124.2
082200     MOVE "IX-3, 1.3.4, (1) a. " TO RE-MARK.                      IX1124.2
082300     MOVE "I-O STATUS IS NOT 00" TO COMPUTED-A.                   IX1124.2
082400     MOVE "00" TO CORRECT-X.                                      IX1124.2
082500     PERFORM FAIL.                                                IX1124.2
082600     GO TO REA-WRITE-F1-01-0.                                     IX1124.2
082700 REA-PASS-F1-01-0.                                                IX1124.2
082800     PERFORM PASS.                                                IX1124.2
082900 REA-WRITE-F1-01-0.                                               IX1124.2
083000     PERFORM PRINT-DETAIL.                                        IX1124.2
083100                                                                  IX1124.2
083200******************************************************************IX1124.2
083300*   TEST  7                                                      *IX1124.2
083400*         REWRITE   (WITH WRONG RECORD LENGTH (SHORTER))         *IX1124.2
083500*         IX-5, 1.3.4 (5) d 1 & 2                                *IX1124.2
083600*         FILE STATUS  00   OR  44   EXPECTED                    *IX1124.2
083700*    KEY:        000000005                                       *IX1124.2
083800******************************************************************IX1124.2
083900 RWR-INIT-GF-01-0.                                                IX1124.2
084000     ADD  1 TO TEST-NUMBER.                                       IX1124.2
084100     CLOSE IX-VS2.                                                IX1124.2
084200     OPEN I-O IX-VS2.                                             IX1124.2
084300     MOVE SPACES TO IX-VS2-STATUS.                                IX1124.2
084400     MOVE 0 TO STATUS-TEST-00.                                    IX1124.2
084500     MOVE "RWRTE SH. 00/44 EXP." TO FEATURE.                      IX1124.2
084600     MOVE "RWR-TEST-GF-01-0" TO PAR-NAME.                         IX1124.2
084700     READ IX-VS2 AT END       GO TO RWR-TEST-GF-01-0.             IX1124.2
084800     READ IX-VS2 AT END       GO TO RWR-TEST-GF-01-0.             IX1124.2
084900     READ IX-VS2 AT END       GO TO RWR-TEST-GF-01-0.             IX1124.2
085000     READ IX-VS2 AT END       GO TO RWR-TEST-GF-01-0.             IX1124.2
085100     READ IX-VS2 AT END       GO TO RWR-TEST-GF-01-0.             IX1124.2
085200 RWR-TEST-GF-01-0.                                                IX1124.2
085300     MOVE "WRONG RECORD LENGTH ( SHORTER)" TO IX-VS2-REC-LONG.    IX1124.2
085400     REWRITE IX-VS2R1-F-G-200.                                    IX1124.2
085500 RWR-TEST-GF-01-1.                                                IX1124.2
085600     IF IX-VS2-STATUS  = "00"                                     IX1124.2
085700         GO TO RWR-PASS-GF-01-0.                                  IX1124.2
085800     IF IX-VS2-STATUS  = "44"                                     IX1124.2
085900         GO TO RWR-PASS-GF-01-0.                                  IX1124.2
086000 RWR-FAIL-GF-01-0.                                                IX1124.2
086100     MOVE "IX-5, 1.3.4, (5) d 1 & 2;  SHORT RECORD" TO RE-MARK.   IX1124.2
086200     PERFORM FAIL.                                                IX1124.2
086300     MOVE  IX-VS2-STATUS      TO COMPUTED-A.                      IX1124.2
086400     MOVE "00 OR 44" TO CORRECT-X.                                IX1124.2
086500     GO TO RWR-WRITE-GF-01-0.                                     IX1124.2
086600 RWR-PASS-GF-01-0.                                                IX1124.2
086700     PERFORM PASS.                                                IX1124.2
086800 RWR-WRITE-GF-01-0.                                               IX1124.2
086900     PERFORM PRINT-DETAIL.                                        IX1124.2
087000                                                                  IX1124.2
087100 TERMINATE-ROUTINE.                                               IX1124.2
087200     EXIT.                                                        IX1124.2
087300                                                                  IX1124.2
087400 CCVS-EXIT SECTION.                                               IX1124.2
087500 CCVS-999999.                                                     IX1124.2
087600     GO TO CLOSE-FILES.                                           IX1124.2
      *END-OF,IX112A                                                                  
