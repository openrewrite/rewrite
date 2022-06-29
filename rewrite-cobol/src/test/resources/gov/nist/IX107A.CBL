      *HEADER,COBOL,IX107A                                                            
000100 IDENTIFICATION DIVISION.                                         IX1074.2
000200 PROGRAM-ID.                                                      IX1074.2
000300     IX107A.                                                      IX1074.2
000400****************************************************************  IX1074.2
000500*                                                              *  IX1074.2
000600*    VALIDATION FOR:-                                          *  IX1074.2
000700*                                                              *  IX1074.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IX1074.2
000900*                                                              *  IX1074.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".IX1074.2
001100*                                                              *  IX1074.2
001200****************************************************************  IX1074.2
001300*                                                                 IX1074.2
001400*       THIS ROUTINE TESTS THE FOLLOWING COBOL ELEMENTS FOR PROPERIX1074.2
001500*    SYNTAX WHEN USING AN INDEXED SEQUENTIAL I-O FILE.            IX1074.2
001600*                                                                 IX1074.2
001700*             SAME AREA FILE-NAME-1 FILE-NAME-2                   IX1074.2
001800*             READ .... RECORD AT END ....                        IX1074.2
001900*             READ .... RECORD END ...                            IX1074.2
002000*             READ .... AT END ....                               IX1074.2
002100*             READ .... END ....                                  IX1074.2
002200*             READ .... RECORD INVALID KEY ...                    IX1074.2
002300*             READ .... INVALID KEY ...                           IX1074.2
002400*             READ .... RECORD INVALID ...                        IX1074.2
002500*             READ .... INVALID ...                               IX1074.2
002600*                                                                 IX1074.2
002700*       THERE ARE TWO FILES USED IN THIS ROUTINE. FOLLOWING       IX1074.2
002800*    CREATION OF EACH FILE THE ROUTINE READS AND VERIFIES THE FILEIX1074.2
002900*    BEFORE ANY OF THE ABOVE TESTS ARE MADE.  ONE FILE SPECIFIES  IX1074.2
003000*    AN ACCESS MODE AS RANDOM AND THE OTHER FILE SPECIFIES AN     IX1074.2
003100*    ACCESS MODE AS SEQUENTIAL.  THE FILES REFERENCED IN THE SAME IX1074.2
003200*    CLAUSE NEED NOT HAVE THE SAME ACCESS MODE.                   IX1074.2
003300*                                                                 IX1074.2
003400*        REFERENCES: SECTION IX-15, SEE VII-19 2.13.3 (4) SAME    IX1074.2
003500*                    AREA                                         IX1074.2
003600*                                                                 IX1074.2
003700*       X-CARDS  WHICH MUST BE REPLACED FOR THIS PROGRAM ARE      IX1074.2
003800*                                                                 IX1074.2
003900*             X-24   INDEXED FILE IMPLEMENTOR-NAME IN ASSGN TO    IX1074.2
004000*                    CLAUSE FOR DATA FILE IX-FS1                  IX1074.2
004100*             X-25   INDEXED FILE IMPLEMENTOR-NAME IN ASSIGN TO   IX1074.2
004200*                    CLAUSE FOR DATA FILE IX-FD2                  IX1074.2
004300*             X-44   INDEXED FILE IMPLEMENTOR-NAME IN ASSGN TO    IX1074.2
004400*                    CLAUSE FOR INDEX FILE IX-FS1                 IX1074.2
004500*             X-45   INDEXED FILE IMPLEMENTOR-NAME IN ASSIGN TO   IX1074.2
004600*                    CLAUSE FOR INDEX FILE IX-FD2                 IX1074.2
004700*             X-55   IMPLEMENTOR-NAME FOR SYSTEM PRINTER          IX1074.2
004800*             X-62   FOR RAW-DATA                                 IX1074.2
004900*             X-82   IMPLEMENTOR-NAME FOR SOURCE-COMPUTER         IX1074.2
005000*             X-83   IMPLEMENTOR-NAME FOR OBJECT-COMPUTER         IX1074.2
005100*             X-84   PRINTER-FILE LABELS (OPTIONAL)  C            IX1074.2
005200*                                                                 IX1074.2
005300*        NOTE:  X-CARDS 44,45, 62 AND 84      ARE OPTIONAL        IX1074.2
005400*               AND NEED ONLY TO BE PRESENT IF THE COMPILER RE-   IX1074.2
005500*               QUIRES THIS CODE BE AVAILABLE FOR PROPER PROGRAM  IX1074.2
005600*               COMPILATION AND EXECUTION. IF THE VP-ROUTINE IS   IX1074.2
005700*               USED THE  X-CARDS MAY BE AUTOMATICALLY SELECTED   IX1074.2
005800*               FOR INCLUSION IN THE PROGRAM BY SPECIFYING THE    IX1074.2
005900*               APPROPRIATE LETTER IN THE "*OPT" VP-ROUTINE       IX1074.2
006000*               CONTROL CARD. THE LETTER  CORRESPONDS TO A        IX1074.2
006100*               CHARACTER IN POSITION 7 OF THE SOURCE LINE AND    IX1074.2
006200*               THEY ARE AS FOLLOWS                               IX1074.2
006300*                                                                 IX1074.2
006400*                  C  SELECTS OBSOLETE FEATURES (E.G. LABEL ..)   IX1074.2
006500*                  J  SELECTS X-CARDS 44 AND 45                   IX1074.2
006600*                                                                 IX1074.2
006700*        NOTE:  THERE IS OPTIONAL SOURCE CODE IN THIS PROGRAM     IX1074.2
006800*               FOR THE CONVENIENCE OF THE USER.  THIS OPTIONAL   IX1074.2
006900*               CODE IS IDENTIFIED BY THE LETTER T   OR U IN      IX1074.2
007000*               POSITION 7  OF THE SOURCE LINE. FOR CODE          IX1074.2
007100*               WITH LETTERS T OR U ONLY ONE SHOULD BE SELECTED.  IX1074.2
007200*               EITHER THE T"S OR THE U"S SHOULD BE USED EXCLU-   IX1074.2
007300*               SIVELY, NOT BOTH.  THE T"S PROVIDE A 29 CHARACTER IX1074.2
007400*               INDEXED KEY SIZE FOR THE FILE AND THE U"S PROVIDE IX1074.2
007500*               AN INDEXED KEY NOT GREATER THAN 8 CHARACTERS.     IX1074.2
007600*               IF THE VP-ROUTINE IS USED THE APPROPRIATE         IX1074.2
007700*               SOURCE CODE MAY BE SELECTED BY SPECIFYING THE     IX1074.2
007800*               RESPECTIVE LETTER IN THE "*OPT" VP-ROUTINE CONTROLIX1074.2
007900*               CARD.                                             IX1074.2
008000*                                                                 IX1074.2
008100******************************************************            IX1074.2
008200 ENVIRONMENT DIVISION.                                            IX1074.2
008300 CONFIGURATION SECTION.                                           IX1074.2
008400 SOURCE-COMPUTER.                                                 IX1074.2
008500     XXXXX082.                                                    IX1074.2
008600 OBJECT-COMPUTER.                                                 IX1074.2
008700     XXXXX083.                                                    IX1074.2
008800 INPUT-OUTPUT SECTION.                                            IX1074.2
008900 FILE-CONTROL.                                                    IX1074.2
009000*    SELECT RAW-DATA   ASSIGN TO                                  IX1074.2
009100*    XXXXX062                                                     IX1074.2
009200*           ORGANIZATION IS INDEXED                               IX1074.2
009300*           ACCESS MODE IS RANDOM                                 IX1074.2
009400*           RECORD KEY IS RAW-DATA-KEY.                           IX1074.2
009500     SELECT PRINT-FILE ASSIGN TO                                  IX1074.2
009600     XXXXX055.                                                    IX1074.2
009700     SELECT IX-FS1 ASSIGN TO                                      IX1074.2
009800     XXXXX024                                                     IX1074.2
009900*    XXXXX044                                                     IX1074.2
010000     RECORD  KEY IS IX-FS1-KEY                                    IX1074.2
010100     ORGANIZATION IS INDEXED                                      IX1074.2
010200     ACCESS MODE IS SEQUENTIAL.                                   IX1074.2
010300     SELECT IX-FD2 ASSIGN TO                                      IX1074.2
010400     XXXXX025                                                     IX1074.2
010500*    XXXXX045                                                     IX1074.2
010600     RECORD KEY IS IX-FD2-KEY                                     IX1074.2
010700     ORGANIZATION IS INDEXED                                      IX1074.2
010800     ACCESS MODE IS RANDOM.                                       IX1074.2
010900 I-O-CONTROL.                                                     IX1074.2
011000     SAME AREA IX-FS1 IX-FD2.                                     IX1074.2
011100 DATA DIVISION.                                                   IX1074.2
011200 FILE SECTION.                                                    IX1074.2
011300*                                                                 IX1074.2
011400*FD  RAW-DATA.                                                    IX1074.2
011500*                                                                 IX1074.2
011600*01  RAW-DATA-SATZ.                                               IX1074.2
011700*    05  RAW-DATA-KEY        PIC X(6).                            IX1074.2
011800*    05  C-DATE              PIC 9(6).                            IX1074.2
011900*    05  C-TIME              PIC 9(8).                            IX1074.2
012000*    05  C-NO-OF-TESTS       PIC 99.                              IX1074.2
012100*    05  C-OK                PIC 999.                             IX1074.2
012200*    05  C-ALL               PIC 999.                             IX1074.2
012300*    05  C-FAIL              PIC 999.                             IX1074.2
012400*    05  C-DELETED           PIC 999.                             IX1074.2
012500*    05  C-INSPECT           PIC 999.                             IX1074.2
012600*    05  C-NOTE              PIC X(13).                           IX1074.2
012700*    05  C-INDENT            PIC X.                               IX1074.2
012800*    05  C-ABORT             PIC X(8).                            IX1074.2
012900 FD  PRINT-FILE.                                                  IX1074.2
013000 01  PRINT-REC PICTURE X(120).                                    IX1074.2
013100 01  DUMMY-RECORD PICTURE X(120).                                 IX1074.2
013200 FD   IX-FS1                                                      IX1074.2
013300C    LABEL RECORD IS STANDARD                                     IX1074.2
013400C    DATA RECORD IS IX-FS1R1-F-G-240                              IX1074.2
013500     BLOCK  CONTAINS 1 RECORDS                                    IX1074.2
013600     RECORD CONTAINS 240 CHARACTERS.                              IX1074.2
013700 01  IX-FS1R1-F-G-240.                                            IX1074.2
013800     03 IX-FS1-REC-120   PIC X(120).                              IX1074.2
013900     03 IX-FS1-REC-121-240.                                       IX1074.2
014000     05 FILLER           PIC X(8).                                IX1074.2
014100     05 IX-FS1-KEY.                                               IX1074.2
014200         10 IX-FS1-KEYNUM  PIC 9(5).                              IX1074.2
014300*        10 FILLER         PIC X(24).                             IX1074.2
014400*    05 FILLER             PIC X(24).                             IX1074.2
014500     05 FILLER           PIC X(83).                               IX1074.2
014600 FD  IX-FD2                                                       IX1074.2
014700C    LABEL RECORD IS STANDARD                                     IX1074.2
014800C    DATA RECORD IS IX-FD2R1-F-G-240                              IX1074.2
014900     BLOCK  CONTAINS 5 RECORDS                                    IX1074.2
015000     RECORD CONTAINS 240 CHARACTERS.                              IX1074.2
015100 01  IX-FD2R1-F-G-240.                                            IX1074.2
015200     03 IX-FD2-REC-120   PIC X(120).                              IX1074.2
015300     03 IX-FD2-REC-121-240.                                       IX1074.2
015400     05 FILLER           PIC X(8).                                IX1074.2
015500     05 IX-FD2-KEY.                                               IX1074.2
015600         10 IX-FD2-KEYNUM  PIC 9(5).                              IX1074.2
015700*        10 FILLER         PIC X(24).                             IX1074.2
015800*    05 FILLER             PIC X(24).                             IX1074.2
015900     05 FILLER           PIC X(83).                               IX1074.2
016000 WORKING-STORAGE SECTION.                                         IX1074.2
016100 01  WRK-FS1-RECKEY.                                              IX1074.2
016200     03 WRK-DU-05V00-001 PIC 9(5)   VALUE ZERO.                   IX1074.2
016300*    03 WRK-XN-24V00-001 PIC X(24)  VALUE                         IX1074.2
016400*        "123456789012345678901234".                              IX1074.2
016500 01  WRK-FD2-RECKEY.                                              IX1074.2
016600     03 WRK-DU-05V00-002 PIC 9(5)   VALUE ZERO.                   IX1074.2
016700*    03 WRK-XN-24V00-002 PIC X(24)  VALUE                         IX1074.2
016800*        "123456789012345678901234".                              IX1074.2
016900 01  WRK-CS-09V00-001    PIC S9(9)  COMP VALUE ZERO.              IX1074.2
017000 01  FS1-FILE-SIZE       PIC 9(6)   VALUE 750.                    IX1074.2
017100 01  FD2-FILE-SIZE       PIC 9(6)   VALUE 649.                    IX1074.2
017200 01  WRK-CS-09V00 PIC S9(9) USAGE COMP VALUE ZERO.                IX1074.2
017300 01  RECORDS-IN-ERROR PIC S9(5) USAGE COMP VALUE ZERO.            IX1074.2
017400 01  ERROR-FLAG PIC 9 VALUE ZERO.                                 IX1074.2
017500 01  EOF-FLAG   PICTURE 9 VALUE ZERO.                             IX1074.2
017600 01  FILE-RECORD-INFORMATION-REC.                                 IX1074.2
017700     03 FILE-RECORD-INFO-SKELETON.                                IX1074.2
017800        05 FILLER                 PICTURE X(48)       VALUE       IX1074.2
017900             "FILE=      ,RECORD=      /0,RECNO=000000,UPDT=00".  IX1074.2
018000        05 FILLER                 PICTURE X(46)       VALUE       IX1074.2
018100             ",ODO=0000,PGM=     ,LRECL=000000,BLKSIZ  =0000".    IX1074.2
018200        05 FILLER                 PICTURE X(26)       VALUE       IX1074.2
018300             ",LFIL=000000,ORG=  ,LBLR= ".                        IX1074.2
018400        05 FILLER                 PICTURE X(37)       VALUE       IX1074.2
018500             ",RECKEY=                             ".             IX1074.2
018600        05 FILLER                 PICTURE X(38)       VALUE       IX1074.2
018700             ",ALTKEY1=                             ".            IX1074.2
018800        05 FILLER                 PICTURE X(38)       VALUE       IX1074.2
018900             ",ALTKEY2=                             ".            IX1074.2
019000        05 FILLER                 PICTURE X(7)        VALUE SPACE.IX1074.2
019100     03 FILE-RECORD-INFO          OCCURS  10  TIMES.              IX1074.2
019200        05 FILE-RECORD-INFO-P1-120.                               IX1074.2
019300           07 FILLER              PIC X(5).                       IX1074.2
019400           07 XFILE-NAME           PIC X(6).                      IX1074.2
019500           07 FILLER              PIC X(8).                       IX1074.2
019600           07 XRECORD-NAME         PIC X(6).                      IX1074.2
019700           07 FILLER              PIC X(1).                       IX1074.2
019800           07 REELUNIT-NUMBER     PIC 9(1).                       IX1074.2
019900           07 FILLER              PIC X(7).                       IX1074.2
020000           07 XRECORD-NUMBER       PIC 9(6).                      IX1074.2
020100           07 FILLER              PIC X(6).                       IX1074.2
020200           07 UPDATE-NUMBER       PIC 9(2).                       IX1074.2
020300           07 FILLER              PIC X(5).                       IX1074.2
020400           07 ODO-NUMBER          PIC 9(4).                       IX1074.2
020500           07 FILLER              PIC X(5).                       IX1074.2
020600           07 XPROGRAM-NAME        PIC X(5).                      IX1074.2
020700           07 FILLER              PIC X(7).                       IX1074.2
020800           07 XRECORD-LENGTH       PIC 9(6).                      IX1074.2
020900           07 FILLER              PIC X(7).                       IX1074.2
021000           07 CHARS-OR-RECORDS    PIC X(2).                       IX1074.2
021100           07 FILLER              PIC X(1).                       IX1074.2
021200           07 XBLOCK-SIZE          PIC 9(4).                      IX1074.2
021300           07 FILLER              PIC X(6).                       IX1074.2
021400           07 RECORDS-IN-FILE     PIC 9(6).                       IX1074.2
021500           07 FILLER              PIC X(5).                       IX1074.2
021600           07 XFILE-ORGANIZATION   PIC X(2).                      IX1074.2
021700           07 FILLER              PIC X(6).                       IX1074.2
021800           07 XLABEL-TYPE          PIC X(1).                      IX1074.2
021900        05 FILE-RECORD-INFO-P121-240.                             IX1074.2
022000           07 FILLER              PIC X(8).                       IX1074.2
022100           07 XRECORD-KEY          PIC X(29).                     IX1074.2
022200           07 FILLER              PIC X(9).                       IX1074.2
022300           07 ALTERNATE-KEY1      PIC X(29).                      IX1074.2
022400           07 FILLER              PIC X(9).                       IX1074.2
022500           07 ALTERNATE-KEY2      PIC X(29).                      IX1074.2
022600           07 FILLER              PIC X(7).                       IX1074.2
022700 01  TEST-RESULTS.                                                IX1074.2
022800     02 FILLER                   PIC X      VALUE SPACE.          IX1074.2
022900     02 FEATURE                  PIC X(20)  VALUE SPACE.          IX1074.2
023000     02 FILLER                   PIC X      VALUE SPACE.          IX1074.2
023100     02 P-OR-F                   PIC X(5)   VALUE SPACE.          IX1074.2
023200     02 FILLER                   PIC X      VALUE SPACE.          IX1074.2
023300     02  PAR-NAME.                                                IX1074.2
023400       03 FILLER                 PIC X(19)  VALUE SPACE.          IX1074.2
023500       03  PARDOT-X              PIC X      VALUE SPACE.          IX1074.2
023600       03 DOTVALUE               PIC 99     VALUE ZERO.           IX1074.2
023700     02 FILLER                   PIC X(8)   VALUE SPACE.          IX1074.2
023800     02 RE-MARK                  PIC X(61).                       IX1074.2
023900 01  TEST-COMPUTED.                                               IX1074.2
024000     02 FILLER                   PIC X(30)  VALUE SPACE.          IX1074.2
024100     02 FILLER                   PIC X(17)  VALUE                 IX1074.2
024200            "       COMPUTED=".                                   IX1074.2
024300     02 COMPUTED-X.                                               IX1074.2
024400     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          IX1074.2
024500     03 COMPUTED-N               REDEFINES COMPUTED-A             IX1074.2
024600                                 PIC -9(9).9(9).                  IX1074.2
024700     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         IX1074.2
024800     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     IX1074.2
024900     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     IX1074.2
025000     03       CM-18V0 REDEFINES COMPUTED-A.                       IX1074.2
025100         04 COMPUTED-18V0                    PIC -9(18).          IX1074.2
025200         04 FILLER                           PIC X.               IX1074.2
025300     03 FILLER PIC X(50) VALUE SPACE.                             IX1074.2
025400 01  TEST-CORRECT.                                                IX1074.2
025500     02 FILLER PIC X(30) VALUE SPACE.                             IX1074.2
025600     02 FILLER PIC X(17) VALUE "       CORRECT =".                IX1074.2
025700     02 CORRECT-X.                                                IX1074.2
025800     03 CORRECT-A                  PIC X(20) VALUE SPACE.         IX1074.2
025900     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      IX1074.2
026000     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         IX1074.2
026100     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     IX1074.2
026200     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     IX1074.2
026300     03      CR-18V0 REDEFINES CORRECT-A.                         IX1074.2
026400         04 CORRECT-18V0                     PIC -9(18).          IX1074.2
026500         04 FILLER                           PIC X.               IX1074.2
026600     03 FILLER PIC X(2) VALUE SPACE.                              IX1074.2
026700     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     IX1074.2
026800 01  CCVS-C-1.                                                    IX1074.2
026900     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PAIX1074.2
027000-    "SS  PARAGRAPH-NAME                                          IX1074.2
027100-    "       REMARKS".                                            IX1074.2
027200     02 FILLER                     PIC X(20)    VALUE SPACE.      IX1074.2
027300 01  CCVS-C-2.                                                    IX1074.2
027400     02 FILLER                     PIC X        VALUE SPACE.      IX1074.2
027500     02 FILLER                     PIC X(6)     VALUE "TESTED".   IX1074.2
027600     02 FILLER                     PIC X(15)    VALUE SPACE.      IX1074.2
027700     02 FILLER                     PIC X(4)     VALUE "FAIL".     IX1074.2
027800     02 FILLER                     PIC X(94)    VALUE SPACE.      IX1074.2
027900 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       IX1074.2
028000 01  REC-CT                        PIC 99       VALUE ZERO.       IX1074.2
028100 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       IX1074.2
028200 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       IX1074.2
028300 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       IX1074.2
028400 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       IX1074.2
028500 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       IX1074.2
028600 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       IX1074.2
028700 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      IX1074.2
028800 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       IX1074.2
028900 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     IX1074.2
029000 01  CCVS-H-1.                                                    IX1074.2
029100     02  FILLER                    PIC X(39)    VALUE SPACES.     IX1074.2
029200     02  FILLER                    PIC X(42)    VALUE             IX1074.2
029300     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 IX1074.2
029400     02  FILLER                    PIC X(39)    VALUE SPACES.     IX1074.2
029500 01  CCVS-H-2A.                                                   IX1074.2
029600   02  FILLER                        PIC X(40)  VALUE SPACE.      IX1074.2
029700   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  IX1074.2
029800   02  FILLER                        PIC XXXX   VALUE             IX1074.2
029900     "4.2 ".                                                      IX1074.2
030000   02  FILLER                        PIC X(28)  VALUE             IX1074.2
030100            " COPY - NOT FOR DISTRIBUTION".                       IX1074.2
030200   02  FILLER                        PIC X(41)  VALUE SPACE.      IX1074.2
030300                                                                  IX1074.2
030400 01  CCVS-H-2B.                                                   IX1074.2
030500   02  FILLER                        PIC X(15)  VALUE             IX1074.2
030600            "TEST RESULT OF ".                                    IX1074.2
030700   02  TEST-ID                       PIC X(9).                    IX1074.2
030800   02  FILLER                        PIC X(4)   VALUE             IX1074.2
030900            " IN ".                                               IX1074.2
031000   02  FILLER                        PIC X(12)  VALUE             IX1074.2
031100     " HIGH       ".                                              IX1074.2
031200   02  FILLER                        PIC X(22)  VALUE             IX1074.2
031300            " LEVEL VALIDATION FOR ".                             IX1074.2
031400   02  FILLER                        PIC X(58)  VALUE             IX1074.2
031500     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IX1074.2
031600 01  CCVS-H-3.                                                    IX1074.2
031700     02  FILLER                      PIC X(34)  VALUE             IX1074.2
031800            " FOR OFFICIAL USE ONLY    ".                         IX1074.2
031900     02  FILLER                      PIC X(58)  VALUE             IX1074.2
032000     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".IX1074.2
032100     02  FILLER                      PIC X(28)  VALUE             IX1074.2
032200            "  COPYRIGHT   1985 ".                                IX1074.2
032300 01  CCVS-E-1.                                                    IX1074.2
032400     02 FILLER                       PIC X(52)  VALUE SPACE.      IX1074.2
032500     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              IX1074.2
032600     02 ID-AGAIN                     PIC X(9).                    IX1074.2
032700     02 FILLER                       PIC X(45)  VALUE SPACES.     IX1074.2
032800 01  CCVS-E-2.                                                    IX1074.2
032900     02  FILLER                      PIC X(31)  VALUE SPACE.      IX1074.2
033000     02  FILLER                      PIC X(21)  VALUE SPACE.      IX1074.2
033100     02 CCVS-E-2-2.                                               IX1074.2
033200         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      IX1074.2
033300         03 FILLER                   PIC X      VALUE SPACE.      IX1074.2
033400         03 ENDER-DESC               PIC X(44)  VALUE             IX1074.2
033500            "ERRORS ENCOUNTERED".                                 IX1074.2
033600 01  CCVS-E-3.                                                    IX1074.2
033700     02  FILLER                      PIC X(22)  VALUE             IX1074.2
033800            " FOR OFFICIAL USE ONLY".                             IX1074.2
033900     02  FILLER                      PIC X(12)  VALUE SPACE.      IX1074.2
034000     02  FILLER                      PIC X(58)  VALUE             IX1074.2
034100     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IX1074.2
034200     02  FILLER                      PIC X(13)  VALUE SPACE.      IX1074.2
034300     02 FILLER                       PIC X(15)  VALUE             IX1074.2
034400             " COPYRIGHT 1985".                                   IX1074.2
034500 01  CCVS-E-4.                                                    IX1074.2
034600     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      IX1074.2
034700     02 FILLER                       PIC X(4)   VALUE " OF ".     IX1074.2
034800     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      IX1074.2
034900     02 FILLER                       PIC X(40)  VALUE             IX1074.2
035000      "  TESTS WERE EXECUTED SUCCESSFULLY".                       IX1074.2
035100 01  XXINFO.                                                      IX1074.2
035200     02 FILLER                       PIC X(19)  VALUE             IX1074.2
035300            "*** INFORMATION ***".                                IX1074.2
035400     02 INFO-TEXT.                                                IX1074.2
035500       04 FILLER                     PIC X(8)   VALUE SPACE.      IX1074.2
035600       04 XXCOMPUTED                 PIC X(20).                   IX1074.2
035700       04 FILLER                     PIC X(5)   VALUE SPACE.      IX1074.2
035800       04 XXCORRECT                  PIC X(20).                   IX1074.2
035900     02 INF-ANSI-REFERENCE           PIC X(48).                   IX1074.2
036000 01  HYPHEN-LINE.                                                 IX1074.2
036100     02 FILLER  PIC IS X VALUE IS SPACE.                          IX1074.2
036200     02 FILLER  PIC IS X(65)    VALUE IS "************************IX1074.2
036300-    "*****************************************".                 IX1074.2
036400     02 FILLER  PIC IS X(54)    VALUE IS "************************IX1074.2
036500-    "******************************".                            IX1074.2
036600 01  CCVS-PGM-ID                     PIC X(9)   VALUE             IX1074.2
036700     "IX107A".                                                    IX1074.2
036800 PROCEDURE DIVISION.                                              IX1074.2
036900 CCVS1 SECTION.                                                   IX1074.2
037000 OPEN-FILES.                                                      IX1074.2
037100*    OPEN I-O RAW-DATA.                                           IX1074.2
037200*    MOVE CCVS-PGM-ID TO RAW-DATA-KEY.                            IX1074.2
037300*    READ RAW-DATA INVALID KEY GO TO END-E-1.                     IX1074.2
037400*    MOVE "ABORTED " TO C-ABORT.                                  IX1074.2
037500*    ADD 1 TO C-NO-OF-TESTS.                                      IX1074.2
037600*    ACCEPT C-DATE  FROM DATE.                                    IX1074.2
037700*    ACCEPT C-TIME  FROM TIME.                                    IX1074.2
037800*    REWRITE RAW-DATA-SATZ INVALID KEY GO TO END-E-1.             IX1074.2
037900*END-E-1.                                                         IX1074.2
038000*    CLOSE RAW-DATA.                                              IX1074.2
038100     OPEN    OUTPUT PRINT-FILE.                                   IX1074.2
038200     MOVE  CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.  IX1074.2
038300     MOVE    SPACE TO TEST-RESULTS.                               IX1074.2
038400     PERFORM HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.              IX1074.2
038500     MOVE    ZERO TO REC-SKL-SUB.                                 IX1074.2
038600     PERFORM CCVS-INIT-FILE 9 TIMES.                              IX1074.2
038700 CCVS-INIT-FILE.                                                  IX1074.2
038800     ADD     1 TO REC-SKL-SUB.                                    IX1074.2
038900     MOVE    FILE-RECORD-INFO-SKELETON                            IX1074.2
039000          TO FILE-RECORD-INFO (REC-SKL-SUB).                      IX1074.2
039100 CCVS-INIT-EXIT.                                                  IX1074.2
039200     GO TO CCVS1-EXIT.                                            IX1074.2
039300 CLOSE-FILES.                                                     IX1074.2
039400*    OPEN I-O RAW-DATA.                                           IX1074.2
039500*    MOVE CCVS-PGM-ID TO RAW-DATA-KEY.                            IX1074.2
039600*    READ RAW-DATA INVALID KEY GO TO END-E-2.                     IX1074.2
039700*    MOVE "OK.     " TO C-ABORT.                                  IX1074.2
039800*    MOVE PASS-COUNTER TO C-OK.                                   IX1074.2
039900*    MOVE ERROR-HOLD   TO C-ALL.                                  IX1074.2
040000*    MOVE ERROR-COUNTER TO C-FAIL.                                IX1074.2
040100*    MOVE DELETE-COUNTER TO C-DELETED.                            IX1074.2
040200*    MOVE INSPECT-COUNTER TO C-INSPECT.                           IX1074.2
040300*    REWRITE RAW-DATA-SATZ INVALID KEY GO TO END-E-2.             IX1074.2
040400*END-E-2.                                                         IX1074.2
040500*    CLOSE RAW-DATA.                                              IX1074.2
040600     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   IX1074.2
040700 TERMINATE-CCVS.                                                  IX1074.2
040800*    EXIT PROGRAM.                                                IX1074.2
040900*TERMINATE-CALL.                                                  IX1074.2
041000     STOP     RUN.                                                IX1074.2
041100 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         IX1074.2
041200 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           IX1074.2
041300 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          IX1074.2
041400 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      IX1074.2
041500     MOVE "****TEST DELETED****" TO RE-MARK.                      IX1074.2
041600 PRINT-DETAIL.                                                    IX1074.2
041700     IF REC-CT NOT EQUAL TO ZERO                                  IX1074.2
041800             MOVE "." TO PARDOT-X                                 IX1074.2
041900             MOVE REC-CT TO DOTVALUE.                             IX1074.2
042000     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      IX1074.2
042100     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               IX1074.2
042200        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 IX1074.2
042300          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 IX1074.2
042400     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              IX1074.2
042500     MOVE SPACE TO CORRECT-X.                                     IX1074.2
042600     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         IX1074.2
042700     MOVE     SPACE TO RE-MARK.                                   IX1074.2
042800 HEAD-ROUTINE.                                                    IX1074.2
042900     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  IX1074.2
043000     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  IX1074.2
043100     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  IX1074.2
043200     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  IX1074.2
043300 COLUMN-NAMES-ROUTINE.                                            IX1074.2
043400     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IX1074.2
043500     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IX1074.2
043600     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        IX1074.2
043700 END-ROUTINE.                                                     IX1074.2
043800     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.IX1074.2
043900 END-RTN-EXIT.                                                    IX1074.2
044000     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IX1074.2
044100 END-ROUTINE-1.                                                   IX1074.2
044200      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      IX1074.2
044300      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               IX1074.2
044400      ADD PASS-COUNTER TO ERROR-HOLD.                             IX1074.2
044500*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   IX1074.2
044600      MOVE PASS-COUNTER TO CCVS-E-4-1.                            IX1074.2
044700      MOVE ERROR-HOLD TO CCVS-E-4-2.                              IX1074.2
044800      MOVE CCVS-E-4 TO CCVS-E-2-2.                                IX1074.2
044900      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           IX1074.2
045000  END-ROUTINE-12.                                                 IX1074.2
045100      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        IX1074.2
045200     IF       ERROR-COUNTER IS EQUAL TO ZERO                      IX1074.2
045300         MOVE "NO " TO ERROR-TOTAL                                IX1074.2
045400         ELSE                                                     IX1074.2
045500         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       IX1074.2
045600     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           IX1074.2
045700     PERFORM WRITE-LINE.                                          IX1074.2
045800 END-ROUTINE-13.                                                  IX1074.2
045900     IF DELETE-COUNTER IS EQUAL TO ZERO                           IX1074.2
046000         MOVE "NO " TO ERROR-TOTAL  ELSE                          IX1074.2
046100         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      IX1074.2
046200     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   IX1074.2
046300     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IX1074.2
046400      IF   INSPECT-COUNTER EQUAL TO ZERO                          IX1074.2
046500          MOVE "NO " TO ERROR-TOTAL                               IX1074.2
046600      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   IX1074.2
046700      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            IX1074.2
046800      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          IX1074.2
046900     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IX1074.2
047000 WRITE-LINE.                                                      IX1074.2
047100     ADD 1 TO RECORD-COUNT.                                       IX1074.2
047200*    IF RECORD-COUNT GREATER 42                                   IX1074.2
047300*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          IX1074.2
047400*        MOVE SPACE TO DUMMY-RECORD                               IX1074.2
047500*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  IX1074.2
047600*        MOVE CCVS-H-1  TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES    IX1074.2
047700*        MOVE CCVS-H-2A TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES    IX1074.2
047800*        MOVE CCVS-H-2B TO DUMMY-RECORD PERFORM WRT-LN 3 TIMES    IX1074.2
047900*        MOVE CCVS-H-3  TO DUMMY-RECORD PERFORM WRT-LN 3 TIMES    IX1074.2
048000*        MOVE CCVS-C-1  TO DUMMY-RECORD PERFORM WRT-LN            IX1074.2
048100*        MOVE CCVS-C-2  TO DUMMY-RECORD PERFORM WRT-LN            IX1074.2
048200*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          IX1074.2
048300*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          IX1074.2
048400*        MOVE ZERO TO RECORD-COUNT.                               IX1074.2
048500     PERFORM WRT-LN.                                              IX1074.2
048600 WRT-LN.                                                          IX1074.2
048700     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               IX1074.2
048800     MOVE SPACE TO DUMMY-RECORD.                                  IX1074.2
048900 BLANK-LINE-PRINT.                                                IX1074.2
049000     PERFORM WRT-LN.                                              IX1074.2
049100 FAIL-ROUTINE.                                                    IX1074.2
049200     IF     COMPUTED-X NOT EQUAL TO SPACE                         IX1074.2
049300            GO TO   FAIL-ROUTINE-WRITE.                           IX1074.2
049400     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.IX1074.2
049500     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 IX1074.2
049600     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   IX1074.2
049700     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IX1074.2
049800     MOVE   SPACES TO INF-ANSI-REFERENCE.                         IX1074.2
049900     GO TO  FAIL-ROUTINE-EX.                                      IX1074.2
050000 FAIL-ROUTINE-WRITE.                                              IX1074.2
050100     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         IX1074.2
050200     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 IX1074.2
050300     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. IX1074.2
050400     MOVE   SPACES TO COR-ANSI-REFERENCE.                         IX1074.2
050500 FAIL-ROUTINE-EX. EXIT.                                           IX1074.2
050600 BAIL-OUT.                                                        IX1074.2
050700     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   IX1074.2
050800     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           IX1074.2
050900 BAIL-OUT-WRITE.                                                  IX1074.2
051000     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  IX1074.2
051100     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 IX1074.2
051200     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IX1074.2
051300     MOVE   SPACES TO INF-ANSI-REFERENCE.                         IX1074.2
051400 BAIL-OUT-EX. EXIT.                                               IX1074.2
051500 CCVS1-EXIT.                                                      IX1074.2
051600     EXIT.                                                        IX1074.2
051700 SECT-IX107A-001 SECTION.                                         IX1074.2
051800 WRITE-INIT-GF-01.                                                IX1074.2
051900     OPEN     OUTPUT  IX-FS1.                                     IX1074.2
052000     MOVE     ZERO TO WRK-CS-09V00-001.                           IX1074.2
052100     MOVE     ZERO TO WRK-DU-05V00-001.                           IX1074.2
052200     MOVE     "IX-FS1"  TO XFILE-NAME (1).                        IX1074.2
052300     MOVE     "R1-F-G"  TO XRECORD-NAME (1).                      IX1074.2
052400     MOVE     00001   TO XRECORD-NUMBER (1).                      IX1074.2
052500     MOVE     CCVS-PGM-ID TO XPROGRAM-NAME (1).                   IX1074.2
052600     MOVE     750     TO RECORDS-IN-FILE (1).                     IX1074.2
052700     MOVE     240     TO XRECORD-LENGTH (1).                      IX1074.2
052800     MOVE     0001    TO XBLOCK-SIZE (1).                         IX1074.2
052900     MOVE     "RC"    TO  CHARS-OR-RECORDS (1).                   IX1074.2
053000     MOVE     "IX"    TO XFILE-ORGANIZATION (1).                  IX1074.2
053100     MOVE     "S"     TO XLABEL-TYPE (1).                         IX1074.2
053200     MOVE     "FILE CREATED"  TO RE-MARK.                         IX1074.2
053300     MOVE     "WRITE-TEST-GF-01" TO PAR-NAME.                     IX1074.2
053400     MOVE    "WRITE  SEQUENTIAL"  TO FEATURE.                     IX1074.2
053500     MOVE     ZERO    TO REC-CT.                                  IX1074.2
053600 WRITE-TEST-GF-01-R.                                              IX1074.2
053700     MOVE     XRECORD-NUMBER (1)  TO WRK-DU-05V00-001.            IX1074.2
053800     MOVE     WRK-FS1-RECKEY TO XRECORD-KEY (1).                  IX1074.2
053900     MOVE     FILE-RECORD-INFO (1)  TO IX-FS1R1-F-G-240.          IX1074.2
054000     WRITE    IX-FS1R1-F-G-240                                    IX1074.2
054100                      INVALID KEY GO TO WRITE-TEST-GF-01-1.       IX1074.2
054200     IF       XRECORD-NUMBER (1)  NOT LESS THAN FS1-FILE-SIZE     IX1074.2
054300              GO TO WRITE-TEST-GF-01-1.                           IX1074.2
054400     ADD      0001  TO XRECORD-NUMBER (1).                        IX1074.2
054500     GO TO    WRITE-TEST-GF-01-R.                                 IX1074.2
054600 WRITE-TEST-GF-01-1.                                              IX1074.2
054700     MOVE     XRECORD-NUMBER (1)  TO COMPUTED-18V0.               IX1074.2
054800     MOVE     FS1-FILE-SIZE TO CORRECT-18V0.                      IX1074.2
054900     IF       XRECORD-NUMBER (1) EQUAL TO FS1-FILE-SIZE           IX1074.2
055000              PERFORM  PASS                                       IX1074.2
055100          ELSE                                                    IX1074.2
055200              MOVE "IX-41 4.9.2        " TO RE-MARK               IX1074.2
055300                   PERFORM FAIL.                                  IX1074.2
055400     PERFORM  PRINT-DETAIL.                                       IX1074.2
055500     CLOSE    IX-FS1.                                             IX1074.2
055600 READ-INIT-F1-01.                                                 IX1074.2
055700     MOVE ZERO TO WRK-CS-09V00.                                   IX1074.2
055800*        THIS TEST READS AND CHECKS THE FILE CREATED IN           IX1074.2
055900*    READ-TEST-001.                                               IX1074.2
056000     OPEN INPUT IX-FS1.                                           IX1074.2
056100 READ-TEST-F1-01.                                                 IX1074.2
056200     READ IX-FS1                                                  IX1074.2
056300          AT END GO TO READ-TEST-F1-01-1.                         IX1074.2
056400     MOVE   IX-FS1R1-F-G-240 TO FILE-RECORD-INFO (1).             IX1074.2
056500     ADD 1 TO WRK-CS-09V00.                                       IX1074.2
056600     IF WRK-CS-09V00 GREATER THAN 750                             IX1074.2
056700        MOVE "MORE THAN 750 RECORDS" TO RE-MARK                   IX1074.2
056800        GO TO READ-FAIL-F1-01.                                    IX1074.2
056900     IF WRK-CS-09V00 NOT EQUAL TO XRECORD-NUMBER (1)              IX1074.2
057000         ADD 1 TO RECORDS-IN-ERROR                                IX1074.2
057100         GO TO READ-TEST-F1-01.                                   IX1074.2
057200     IF XFILE-NAME (1) NOT EQUAL TO "IX-FS1"                      IX1074.2
057300        ADD 1 TO RECORDS-IN-ERROR                                 IX1074.2
057400        GO TO READ-TEST-F1-01.                                    IX1074.2
057500     IF XLABEL-TYPE (1) NOT EQUAL TO "S"                          IX1074.2
057600        ADD 1 TO RECORDS-IN-ERROR.                                IX1074.2
057700     GO TO READ-TEST-F1-01.                                       IX1074.2
057800 READ-TEST-F1-01-1.                                               IX1074.2
057900     IF RECORDS-IN-ERROR EQUAL TO ZERO                            IX1074.2
058000         GO TO READ-PASS-F1-01.                                   IX1074.2
058100     MOVE "ERRORS IN READING IX-FS1" TO RE-MARK.                  IX1074.2
058200 READ-FAIL-F1-01.                                                 IX1074.2
058300     MOVE "RECORDS IN ERROR =" TO COMPUTED-A.                     IX1074.2
058400     MOVE RECORDS-IN-ERROR TO CORRECT-18V0.                       IX1074.2
058500     MOVE "IX-28 4.5.2                               " TO RE-MARK.IX1074.2
058600     PERFORM FAIL.                                                IX1074.2
058700     GO TO READ-WRITE-F1-01.                                      IX1074.2
058800 READ-PASS-F1-01.                                                 IX1074.2
058900     PERFORM PASS.                                                IX1074.2
059000     MOVE "FILE VERIFIED RECS =" TO COMPUTED-A.                   IX1074.2
059100     MOVE WRK-CS-09V00 TO CORRECT-18V0.                           IX1074.2
059200 READ-WRITE-F1-01.                                                IX1074.2
059300     MOVE "READ-TEST-F1-01" TO PAR-NAME.                          IX1074.2
059400     MOVE "READ  TO VERIFY   " TO FEATURE.                        IX1074.2
059500     PERFORM PRINT-DETAIL.                                        IX1074.2
059600 READ-CLOSE-F1-01.                                                IX1074.2
059700     CLOSE IX-FS1.                                                IX1074.2
059800 READ-INIT-F1-02.                                                 IX1074.2
059900     MOVE ZERO TO WRK-CS-09V00.                                   IX1074.2
060000     MOVE ZERO TO RECORDS-IN-ERROR.                               IX1074.2
060100     OPEN INPUT   IX-FS1.                                         IX1074.2
060200*            FOUR OPTIONS FOR THE READ STATEMENT ARE CHECKED      IX1074.2
060300*    IN THIS SERIES OF TESTS.                                     IX1074.2
060400     MOVE "READ...RECORD AT END ..." TO FEATURE.                  IX1074.2
060500     MOVE "READ-TEST-F1-02" TO PAR-NAME.                          IX1074.2
060600     MOVE ZERO TO ERROR-FLAG.                                     IX1074.2
060700 READ-TEST-F1-02.                                                 IX1074.2
060800     READ IX-FS1 RECORD AT END                                    IX1074.2
060900              MOVE "UNEXPECTED EOF" TO COMPUTED-A                 IX1074.2
061000              MOVE 1 TO EOF-FLAG                                  IX1074.2
061100              GO TO READ-FAIL-F1-02.                              IX1074.2
061200     PERFORM RECORD-CHECK.                                        IX1074.2
061300     IF WRK-CS-09V00 EQUAL TO 200                                 IX1074.2
061400              GO TO READ-TEST-F1-02-1.                            IX1074.2
061500             GO TO READ-TEST-F1-02.                               IX1074.2
061600 RECORD-CHECK.                                                    IX1074.2
061700     MOVE IX-FS1R1-F-G-240 TO FILE-RECORD-INFO (1).               IX1074.2
061800     ADD 1 TO WRK-CS-09V00.                                       IX1074.2
061900     IF WRK-CS-09V00 NOT EQUAL TO XRECORD-NUMBER (1)              IX1074.2
062000         ADD 1 TO RECORDS-IN-ERROR                                IX1074.2
062100         MOVE 1 TO ERROR-FLAG.                                    IX1074.2
062200 READ-TEST-F1-02-1.                                               IX1074.2
062300     IF ERROR-FLAG EQUAL TO ZERO                                  IX1074.2
062400         GO TO READ-PASS-F1-02.                                   IX1074.2
062500     MOVE "ERROR IN RECORD(S)" TO COMPUTED-A.                     IX1074.2
062600 READ-FAIL-F1-02.                                                 IX1074.2
062700     MOVE "IX-28 4.5.2                               " TO RE-MARK.IX1074.2
062800     PERFORM FAIL.                                                IX1074.2
062900     GO TO READ-WRITE-F1-02.                                      IX1074.2
063000 READ-PASS-F1-02.                                                 IX1074.2
063100     PERFORM PASS.                                                IX1074.2
063200 READ-WRITE-F1-02.                                                IX1074.2
063300     PERFORM PRINT-DETAIL.                                        IX1074.2
063400 READ-INIT-F1-03.                                                 IX1074.2
063500     IF EOF-FLAG EQUAL TO 1                                       IX1074.2
063600        GO TO READ-EOF-F1-06.                                     IX1074.2
063700     MOVE ZERO TO ERROR-FLAG.                                     IX1074.2
063800     MOVE "READ...AT END..." TO FEATURE.                          IX1074.2
063900     MOVE "READ-TEST-F1-03" TO PAR-NAME.                          IX1074.2
064000 READ-TEST-F1-03.                                                 IX1074.2
064100     READ IX-FS1 AT END                                           IX1074.2
064200         MOVE "UNEXPECTED EOF" TO COMPUTED-A                      IX1074.2
064300         MOVE 1 TO EOF-FLAG                                       IX1074.2
064400         GO TO READ-FAIL-F1-03.                                   IX1074.2
064500     PERFORM RECORD-CHECK.                                        IX1074.2
064600     IF WRK-CS-09V00 EQUAL TO 400                                 IX1074.2
064700         GO TO READ-TEST-F1-03-1.                                 IX1074.2
064800     GO TO READ-TEST-F1-03.                                       IX1074.2
064900 READ-TEST-F1-03-1.                                               IX1074.2
065000     IF ERROR-FLAG EQUAL TO ZERO                                  IX1074.2
065100         GO TO READ-PASS-F1-03.                                   IX1074.2
065200     MOVE "ERROR IN RECORD(S)" TO COMPUTED-A.                     IX1074.2
065300 READ-FAIL-F1-03.                                                 IX1074.2
065400     MOVE "IX-28 4.5.2                               " TO RE-MARK.IX1074.2
065500     PERFORM FAIL.                                                IX1074.2
065600     GO TO READ-WRITE-F1-03.                                      IX1074.2
065700 READ-PASS-F1-03.                                                 IX1074.2
065800     PERFORM PASS.                                                IX1074.2
065900 READ-WRITE-F1-03.                                                IX1074.2
066000     PERFORM PRINT-DETAIL.                                        IX1074.2
066100 READ-INIT-F1-04.                                                 IX1074.2
066200     IF EOF-FLAG EQUAL TO 1                                       IX1074.2
066300        GO TO READ-EOF-F1-06.                                     IX1074.2
066400     MOVE ZERO TO ERROR-FLAG.                                     IX1074.2
066500     MOVE "READ...RECORD END..." TO FEATURE.                      IX1074.2
066600     MOVE "READ-TEST-F1-04" TO PAR-NAME.                          IX1074.2
066700 READ-TEST-F1-04.                                                 IX1074.2
066800     READ IX-FS1 RECORD END                                       IX1074.2
066900          MOVE "UNEXPECTED EOF" TO COMPUTED-A                     IX1074.2
067000          MOVE 1 TO EOF-FLAG                                      IX1074.2
067100          GO TO READ-FAIL-F1-04.                                  IX1074.2
067200     PERFORM RECORD-CHECK.                                        IX1074.2
067300     IF WRK-CS-09V00 EQUAL TO 600                                 IX1074.2
067400         GO TO READ-TEST-F1-04-1.                                 IX1074.2
067500     GO TO READ-TEST-F1-04.                                       IX1074.2
067600 READ-TEST-F1-04-1.                                               IX1074.2
067700     IF ERROR-FLAG EQUAL TO ZERO                                  IX1074.2
067800         GO TO READ-PASS-F1-04.                                   IX1074.2
067900     MOVE "ERROR IN RECORD(S)" TO COMPUTED-A.                     IX1074.2
068000 READ-FAIL-F1-04.                                                 IX1074.2
068100     MOVE "IX-28 4.5.2                               " TO RE-MARK.IX1074.2
068200     PERFORM FAIL.                                                IX1074.2
068300     GO TO READ-WRITE-F1-04.                                      IX1074.2
068400 READ-PASS-F1-04.                                                 IX1074.2
068500     PERFORM PASS.                                                IX1074.2
068600 READ-WRITE-F1-04.                                                IX1074.2
068700     PERFORM PRINT-DETAIL.                                        IX1074.2
068800 READ-INIT-F1-05.                                                 IX1074.2
068900     IF EOF-FLAG EQUAL TO 1                                       IX1074.2
069000         GO TO READ-EOF-F1-06.                                    IX1074.2
069100     MOVE ZERO TO ERROR-FLAG.                                     IX1074.2
069200     MOVE "READ...END..." TO RE-MARK.                             IX1074.2
069300     MOVE  "READ-TEST-F1-05" TO PAR-NAME.                         IX1074.2
069400 READ-TEST-F1-05.                                                 IX1074.2
069500     READ IX-FS1 END GO TO READ-TEST-F1-05-1.                     IX1074.2
069600     PERFORM RECORD-CHECK.                                        IX1074.2
069700     IF WRK-CS-09V00 GREATER THAN 750                             IX1074.2
069800          GO TO READ-TEST-F1-05-1.                                IX1074.2
069900     GO TO READ-TEST-F1-05.                                       IX1074.2
070000 READ-TEST-F1-05-1.                                               IX1074.2
070100     IF ERROR-FLAG EQUAL TO ZERO                                  IX1074.2
070200          GO TO READ-PASS-F1-05.                                  IX1074.2
070300 READ-FAIL-F1-05.                                                 IX1074.2
070400     MOVE "ERROR IN RECORD(S)" TO COMPUTED-A.                     IX1074.2
070500     MOVE "IX-28 4.5.2                               " TO RE-MARK.IX1074.2
070600     PERFORM FAIL.                                                IX1074.2
070700     GO TO READ-WRITE-F1-05.                                      IX1074.2
070800 READ-PASS-F1-05.                                                 IX1074.2
070900     PERFORM PASS.                                                IX1074.2
071000 READ-WRITE-F1-05.                                                IX1074.2
071100     PERFORM PRINT-DETAIL.                                        IX1074.2
071200 READ-TEST-F1-06.                                                 IX1074.2
071300     IF RECORDS-IN-ERROR NOT EQUAL TO ZERO                        IX1074.2
071400          MOVE "RECORDS IN ERROR =" TO COMPUTED-A                 IX1074.2
071500          MOVE RECORDS-IN-ERROR TO CORRECT-18V0                   IX1074.2
071600          GO TO READ-FAIL-F1-06.                                  IX1074.2
071700     IF WRK-CS-09V00 GREATER THAN 750                             IX1074.2
071800          MOVE "MORE THAN 750 RECORDS" TO RE-MARK                 IX1074.2
071900          GO TO READ-FAIL-F1-06.                                  IX1074.2
072000 READ-PASS-F1-06.                                                 IX1074.2
072100     PERFORM PASS.                                                IX1074.2
072200     GO TO READ-WRITE-F1-06.                                      IX1074.2
072300 READ-EOF-F1-06.                                                  IX1074.2
072400     MOVE "LESS THAN 750 RECORDS" TO RE-MARK.                     IX1074.2
072500     MOVE "RECORDS READ =" TO COMPUTED-A.                         IX1074.2
072600     MOVE WRK-CS-09V00 TO CORRECT-18V0.                           IX1074.2
072700 READ-FAIL-F1-06.                                                 IX1074.2
072800     MOVE "IX-28 4.5.2                               " TO RE-MARK.IX1074.2
072900     PERFORM FAIL.                                                IX1074.2
073000 READ-WRITE-F1-06.                                                IX1074.2
073100     MOVE "READ-TEST-F1-06" TO PAR-NAME.                          IX1074.2
073200     MOVE "READ IX-FS1 750R" TO FEATURE.                          IX1074.2
073300     PERFORM PRINT-DETAIL.                                        IX1074.2
073400 READ-CLOSE-F1-06.                                                IX1074.2
073500     CLOSE IX-FS1.                                                IX1074.2
073600 SECT-IX107A-002 SECTION.                                         IX1074.2
073700 WRITE-INIT-GF-02.                                                IX1074.2
073800     OPEN     OUTPUT  IX-FD2.                                     IX1074.2
073900     MOVE     ZERO TO WRK-CS-09V00-001.                           IX1074.2
074000     MOVE     ZERO TO WRK-DU-05V00-002.                           IX1074.2
074100     MOVE     "IX-FD2"  TO XFILE-NAME (2).                        IX1074.2
074200     MOVE     "R1-F-G"  TO XRECORD-NAME (2).                      IX1074.2
074300     MOVE     00001   TO XRECORD-NUMBER (2).                      IX1074.2
074400     MOVE     CCVS-PGM-ID TO XPROGRAM-NAME (2).                   IX1074.2
074500     MOVE     649     TO RECORDS-IN-FILE (2).                     IX1074.2
074600     MOVE     240     TO XRECORD-LENGTH (2).                      IX1074.2
074700     MOVE     0005    TO XBLOCK-SIZE (2).                         IX1074.2
074800     MOVE     "RC"    TO  CHARS-OR-RECORDS (2).                   IX1074.2
074900     MOVE     "IX"    TO XFILE-ORGANIZATION (2).                  IX1074.2
075000     MOVE     "S"     TO XLABEL-TYPE (2).                         IX1074.2
075100     MOVE     "FILE CREATED"  TO RE-MARK.                         IX1074.2
075200     MOVE     "WRITE-TEST-GF-02" TO PAR-NAME.                     IX1074.2
075300     MOVE     "WRITE RANDOM MODE"  TO FEATURE.                    IX1074.2
075400     MOVE     ZERO    TO REC-CT.                                  IX1074.2
075500 WRITE-TEST-GF-02-R.                                              IX1074.2
075600     MOVE     XRECORD-NUMBER (2)  TO WRK-DU-05V00-002.            IX1074.2
075700     MOVE     WRK-FD2-RECKEY TO XRECORD-KEY (2).                  IX1074.2
075800     MOVE     FILE-RECORD-INFO (2)  TO IX-FD2R1-F-G-240.          IX1074.2
075900     WRITE    IX-FD2R1-F-G-240                                    IX1074.2
076000                      INVALID KEY GO TO WRITE-TEST-GF-02-1.       IX1074.2
076100     IF       XRECORD-NUMBER (2)  NOT LESS THAN FD2-FILE-SIZE     IX1074.2
076200              GO TO WRITE-TEST-GF-02-1.                           IX1074.2
076300     ADD      0001  TO XRECORD-NUMBER (2).                        IX1074.2
076400     GO TO    WRITE-TEST-GF-02-R.                                 IX1074.2
076500 WRITE-TEST-GF-02-1.                                              IX1074.2
076600     MOVE     XRECORD-NUMBER (2)  TO COMPUTED-18V0.               IX1074.2
076700     MOVE     FD2-FILE-SIZE TO CORRECT-18V0.                      IX1074.2
076800     IF       XRECORD-NUMBER (2) EQUAL TO FD2-FILE-SIZE           IX1074.2
076900              PERFORM  PASS                                       IX1074.2
077000              ELSE                                                IX1074.2
077100              MOVE "IX-41 4.9.2                     " TO RE-MARK  IX1074.2
077200                   PERFORM FAIL.                                  IX1074.2
077300     PERFORM  PRINT-DETAIL.                                       IX1074.2
077400     CLOSE    IX-FD2.                                             IX1074.2
077500 READ-INIT-F2-07.                                                 IX1074.2
077600     MOVE   ZERO TO WRK-DU-05V00-002.                             IX1074.2
077700     MOVE ZERO TO WRK-CS-09V00.                                   IX1074.2
077800     MOVE    ZERO TO RECORDS-IN-ERROR.                            IX1074.2
077900*        THIS TEST READS AND CHECKS THE FILE CREATED IN           IX1074.2
078000*    READ-TEST-GF-02.                                             IX1074.2
078100     OPEN INPUT IX-FD2.                                           IX1074.2
078200 READ-TEST-F2-07.                                                 IX1074.2
078300     ADD    00001 TO WRK-DU-05V00-002.                            IX1074.2
078400     MOVE   WRK-FD2-RECKEY TO IX-FD2-KEY.                         IX1074.2
078500     READ IX-FD2 RECORD                                           IX1074.2
078600         INVALID KEY GO TO READ-TEST-F2-07-1.                     IX1074.2
078700     MOVE IX-FD2R1-F-G-240 TO FILE-RECORD-INFO (2).               IX1074.2
078800     ADD 1 TO WRK-CS-09V00.                                       IX1074.2
078900     IF WRK-CS-09V00 GREATER THAN 649                             IX1074.2
079000         MOVE "MORE THAN 649 RECORDS" TO RE-MARK                  IX1074.2
079100         GO TO READ-FAIL-F2-07.                                   IX1074.2
079200     IF WRK-CS-09V00 NOT EQUAL TO XRECORD-NUMBER (2)              IX1074.2
079300         ADD 1 TO RECORDS-IN-ERROR                                IX1074.2
079400         GO TO READ-TEST-F2-07.                                   IX1074.2
079500     IF XFILE-NAME (2) NOT EQUAL TO "IX-FD2"                      IX1074.2
079600         ADD 1 TO RECORDS-IN-ERROR                                IX1074.2
079700         GO TO READ-TEST-F2-07.                                   IX1074.2
079800     IF XLABEL-TYPE (2) NOT EQUAL TO "S"                          IX1074.2
079900     ADD 1 TO RECORDS-IN-ERROR.                                   IX1074.2
080000     GO TO READ-TEST-F2-07.                                       IX1074.2
080100 READ-TEST-F2-07-1.                                               IX1074.2
080200     IF RECORDS-IN-ERROR EQUAL TO ZERO                            IX1074.2
080300          GO TO READ-PASS-F2-07.                                  IX1074.2
080400     MOVE "ERRORS IN READING IX-FD2" TO RE-MARK.                  IX1074.2
080500 READ-FAIL-F2-07.                                                 IX1074.2
080600     MOVE RECORDS-IN-ERROR TO CORRECT-18V0.                       IX1074.2
080700     MOVE "IX-28 4.5.2                               " TO RE-MARK.IX1074.2
080800     PERFORM FAIL.                                                IX1074.2
080900     GO TO READ-READ-F2-07.                                       IX1074.2
081000 READ-PASS-F2-07.                                                 IX1074.2
081100     PERFORM PASS.                                                IX1074.2
081200     MOVE "FILE VERIFIED RECS =" TO COMPUTED-A.                   IX1074.2
081300     MOVE WRK-CS-09V00 TO CORRECT-18V0.                           IX1074.2
081400 READ-READ-F2-07.                                                 IX1074.2
081500     MOVE "READ-TEST-F2-07"  TO PAR-NAME.                         IX1074.2
081600     MOVE "VERIFY FILE IX-FD2" TO FEATURE.                        IX1074.2
081700     PERFORM PRINT-DETAIL.                                        IX1074.2
081800 READ-CLOSE-F2-07.                                                IX1074.2
081900     CLOSE IX-FD2.                                                IX1074.2
082000 READ-INIT-F2-08.                                                 IX1074.2
082100     MOVE   ZERO TO WRK-DU-05V00-002.                             IX1074.2
082200     MOVE ZERO TO WRK-CS-09V00.                                   IX1074.2
082300     MOVE ZERO TO RECORDS-IN-ERROR.                               IX1074.2
082400     OPEN INPUT IX-FD2.                                           IX1074.2
082500*        FOUR OPTIONS FOR THE READ STATEMENT ARE CHECKED          IX1074.2
082600*    IN THIS SERIES OF TESTS.                                     IX1074.2
082700     MOVE "LEV 1 READ STATEMENT" TO FEATURE.                      IX1074.2
082800     MOVE     ZERO TO EOF-FLAG.                                   IX1074.2
082900     MOVE "READ...RECORD INVALID KEY ..." TO FEATURE.             IX1074.2
083000     MOVE "READ-TEST-F2-08" TO PAR-NAME.                          IX1074.2
083100     MOVE ZERO TO ERROR-FLAG.                                     IX1074.2
083200 READ-TEST-F2-08.                                                 IX1074.2
083300     ADD    0001 TO WRK-DU-05V00-002.                             IX1074.2
083400     MOVE   WRK-FD2-RECKEY TO IX-FD2-KEY.                         IX1074.2
083500     READ IX-FD2 RECORD                                           IX1074.2
083600          INVALID KEY MOVE "INVALID KEY" TO COMPUTED-A            IX1074.2
083700          MOVE 1 TO EOF-FLAG                                      IX1074.2
083800          GO TO READ-FAIL-F2-08.                                  IX1074.2
083900     PERFORM RECORD-CHECK-1.                                      IX1074.2
084000     IF WRK-CS-09V00 EQUAL TO 50                                  IX1074.2
084100           GO TO READ-TEST-F2-08-1.                               IX1074.2
084200     GO TO READ-TEST-F2-08.                                       IX1074.2
084300 RECORD-CHECK-1.                                                  IX1074.2
084400     MOVE IX-FD2R1-F-G-240 TO FILE-RECORD-INFO (2).               IX1074.2
084500     ADD 1 TO WRK-CS-09V00.                                       IX1074.2
084600     IF WRK-CS-09V00 NOT EQUAL TO XRECORD-NUMBER (2)              IX1074.2
084700         ADD 1 TO RECORDS-IN-ERROR                                IX1074.2
084800         MOVE 1 TO ERROR-FLAG.                                    IX1074.2
084900 READ-TEST-F2-08-1.                                               IX1074.2
085000     IF ERROR-FLAG EQUAL TO ZERO                                  IX1074.2
085100         GO TO READ-PASS-F2-08.                                   IX1074.2
085200     MOVE "ERROR IN RECORD(S)" TO COMPUTED-A.                     IX1074.2
085300 READ-FAIL-F2-08.                                                 IX1074.2
085400     MOVE "IX-28 4.5.2                               " TO RE-MARK.IX1074.2
085500     PERFORM FAIL.                                                IX1074.2
085600     GO TO READ-WRITE-F2-08.                                      IX1074.2
085700 READ-PASS-F2-08.                                                 IX1074.2
085800     PERFORM PASS.                                                IX1074.2
085900 READ-WRITE-F2-08.                                                IX1074.2
086000     PERFORM PRINT-DETAIL.                                        IX1074.2
086100 READ-INIT-F2-09.                                                 IX1074.2
086200     MOVE   ZERO TO WRK-DU-05V00-002.                             IX1074.2
086300     MOVE      ZERO TO WRK-CS-09V00.                              IX1074.2
086400     MOVE     ZERO TO RECORDS-IN-ERROR.                           IX1074.2
086500     IF EOF-FLAG EQUAL TO 1                                       IX1074.2
086600         GO TO READ-EOF-F2-12.                                    IX1074.2
086700     MOVE ZERO TO ERROR-FLAG.                                     IX1074.2
086800     MOVE "READ...INVALID KEY..." TO FEATURE.                     IX1074.2
086900     MOVE "READ-TEST-F2-09" TO PAR-NAME.                          IX1074.2
087000 READ-TEST-F2-09.                                                 IX1074.2
087100     ADD    00001 TO WRK-DU-05V00-002.                            IX1074.2
087200     MOVE   WRK-FD2-RECKEY TO IX-FD2-KEY.                         IX1074.2
087300     READ IX-FD2  INVALID KEY                                     IX1074.2
087400          MOVE "INVALID KEY" TO COMPUTED-A                        IX1074.2
087500          MOVE 1 TO EOF-FLAG                                      IX1074.2
087600          GO TO READ-FAIL-F2-09.                                  IX1074.2
087700     PERFORM RECORD-CHECK-1.                                      IX1074.2
087800     IF WRK-CS-09V00 EQUAL TO 200                                 IX1074.2
087900         GO TO READ-TEST-F2-09-1.                                 IX1074.2
088000     GO TO READ-TEST-F2-09.                                       IX1074.2
088100 READ-TEST-F2-09-1.                                               IX1074.2
088200     IF ERROR-FLAG EQUAL TO ZERO                                  IX1074.2
088300          GO TO READ-PASS-F2-09.                                  IX1074.2
088400     MOVE "ERROR IN RECORD(S)" TO COMPUTED-A.                     IX1074.2
088500 READ-FAIL-F2-09.                                                 IX1074.2
088600     MOVE "IX-28 4.5.2                               " TO RE-MARK.IX1074.2
088700     PERFORM FAIL.                                                IX1074.2
088800     GO TO READ-WRITE-F2-09.                                      IX1074.2
088900 READ-PASS-F2-09.                                                 IX1074.2
089000     PERFORM PASS.                                                IX1074.2
089100 READ-WRITE-F2-09.                                                IX1074.2
089200     PERFORM PRINT-DETAIL.                                        IX1074.2
089300 READ-INIT-F2-10.                                                 IX1074.2
089400     MOVE   ZERO TO WRK-DU-05V00-002.                             IX1074.2
089500     MOVE      ZERO TO WRK-CS-09V00.                              IX1074.2
089600     MOVE     ZERO TO RECORDS-IN-ERROR.                           IX1074.2
089700     IF EOF-FLAG EQUAL TO 1                                       IX1074.2
089800         GO TO READ-EOF-F2-12.                                    IX1074.2
089900     MOVE ZERO TO ERROR-FLAG.                                     IX1074.2
090000     MOVE "READ...RECORD INVALID..." TO FEATURE.                  IX1074.2
090100     MOVE "READ-TEST-F2-10" TO PAR-NAME.                          IX1074.2
090200 READ-TEST-F2-10.                                                 IX1074.2
090300     ADD    0001 TO WRK-DU-05V00-002.                             IX1074.2
090400     MOVE   WRK-FD2-RECKEY TO IX-FD2-KEY.                         IX1074.2
090500     READ IX-FD2 RECORD INVALID                                   IX1074.2
090600          MOVE "INVALID KEY" TO COMPUTED-A                        IX1074.2
090700          MOVE 1 TO EOF-FLAG                                      IX1074.2
090800          GO TO READ-FAIL-F2-10.                                  IX1074.2
090900     PERFORM RECORD-CHECK-1.                                      IX1074.2
091000     IF WRK-CS-09V00 EQUAL TO 499                                 IX1074.2
091100          GO TO READ-TEST-F2-10-1.                                IX1074.2
091200     GO TO READ-TEST-F2-10.                                       IX1074.2
091300 READ-TEST-F2-10-1.                                               IX1074.2
091400     IF ERROR-FLAG EQUAL TO ZERO                                  IX1074.2
091500           GO TO READ-PASS-F2-10.                                 IX1074.2
091600     MOVE "ERROR IN RECORD(S)" TO COMPUTED-A.                     IX1074.2
091700 READ-FAIL-F2-10.                                                 IX1074.2
091800     MOVE "IX-28 4.5.2                               " TO RE-MARK.IX1074.2
091900     PERFORM FAIL.                                                IX1074.2
092000     GO TO READ-WRITE-F2-10.                                      IX1074.2
092100 READ-PASS-F2-10.                                                 IX1074.2
092200     PERFORM PASS.                                                IX1074.2
092300 READ-WRITE-F2-10.                                                IX1074.2
092400     PERFORM PRINT-DETAIL.                                        IX1074.2
092500 READ-INIT-F2-11.                                                 IX1074.2
092600     MOVE   ZERO TO WRK-DU-05V00-002.                             IX1074.2
092700     MOVE      ZERO TO WRK-CS-09V00.                              IX1074.2
092800     MOVE     ZERO TO RECORDS-IN-ERROR.                           IX1074.2
092900     IF EOF-FLAG EQUAL TO 1                                       IX1074.2
093000         GO TO READ-EOF-F2-12.                                    IX1074.2
093100     MOVE ZERO TO ERROR-FLAG.                                     IX1074.2
093200     MOVE "READ...INVALID..." TO FEATURE.                         IX1074.2
093300     MOVE "READ-TEST-F2-11" TO PAR-NAME.                          IX1074.2
093400 READ-TEST-F2-11.                                                 IX1074.2
093500     ADD    0001 TO WRK-DU-05V00-002.                             IX1074.2
093600     MOVE   WRK-FD2-RECKEY TO  IX-FD2-KEY.                        IX1074.2
093700     READ IX-FD2 INVALID                                          IX1074.2
093800          GO TO READ-TEST-F2-11-1.                                IX1074.2
093900     PERFORM RECORD-CHECK-1.                                      IX1074.2
094000     IF WRK-CS-09V00 GREATER THAN 649                             IX1074.2
094100          GO TO READ-TEST-F2-11-1.                                IX1074.2
094200     GO TO READ-TEST-F2-11.                                       IX1074.2
094300 READ-TEST-F2-11-1.                                               IX1074.2
094400     IF ERROR-FLAG EQUAL TO ZERO                                  IX1074.2
094500         GO TO READ-PASS-F2-11.                                   IX1074.2
094600 READ-FAIL-F2-11.                                                 IX1074.2
094700     MOVE "ERROR IN RECORD(S)" TO COMPUTED-A.                     IX1074.2
094800     MOVE "IX-28 4.5.2                               " TO RE-MARK.IX1074.2
094900     PERFORM FAIL.                                                IX1074.2
095000     GO TO READ-WRITE-F2-11.                                      IX1074.2
095100 READ-PASS-F2-11.                                                 IX1074.2
095200     PERFORM PASS.                                                IX1074.2
095300 READ-WRITE-F2-11.                                                IX1074.2
095400     PERFORM PRINT-DETAIL.                                        IX1074.2
095500 READ-TEST-F2-12.                                                 IX1074.2
095600     IF RECORDS-IN-ERROR NOT EQUAL TO ZERO                        IX1074.2
095700          MOVE "RECORDS IN ERROR =" TO COMPUTED-A                 IX1074.2
095800          MOVE RECORDS-IN-ERROR TO CORRECT-18V0                   IX1074.2
095900          GO TO READ-FAIL-F2-12.                                  IX1074.2
096000     IF WRK-CS-09V00 GREATER THAN 649                             IX1074.2
096100          MOVE "MORE THAN 649 RECORDS" TO RE-MARK                 IX1074.2
096200          GO TO READ-FAIL-F2-12.                                  IX1074.2
096300 READ-PASS-F2-12.                                                 IX1074.2
096400     PERFORM PASS                                                 IX1074.2
096500     GO TO READ-WRITE-F2-12.                                      IX1074.2
096600 READ-EOF-F2-12.                                                  IX1074.2
096700     MOVE "LESS THAN 649 RECORDS" TO RE-MARK.                     IX1074.2
096800     MOVE "RECORDS READ =" TO COMPUTED-A.                         IX1074.2
096900     MOVE WRK-CS-09V00 TO CORRECT-18V0.                           IX1074.2
097000 READ-FAIL-F2-12.                                                 IX1074.2
097100     PERFORM FAIL.                                                IX1074.2
097200 READ-WRITE-F2-12.                                                IX1074.2
097300     MOVE "READ-TEST-F2-12" TO PAR-NAME.                          IX1074.2
097400     MOVE "READ IX-FS2 VERIFY" TO FEATURE.                        IX1074.2
097500     PERFORM PRINT-DETAIL.                                        IX1074.2
097600 READ-CLOSE-F2-12.                                                IX1074.2
097700     CLOSE IX-FD2.                                                IX1074.2
097800 TERMINATE-ROUTINE.                                               IX1074.2
097900     EXIT.                                                        IX1074.2
098000 CCVS-EXIT SECTION.                                               IX1074.2
098100 CCVS-999999.                                                     IX1074.2
098200     GO TO CLOSE-FILES.                                           IX1074.2
      *END-OF,IX107A                                                                  
