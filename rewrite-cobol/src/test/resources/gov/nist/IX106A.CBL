      *HEADER,COBOL,IX106A                                                            
000100 IDENTIFICATION DIVISION.                                         IX1064.2
000200 PROGRAM-ID.                                                      IX1064.2
000300     IX106A.                                                      IX1064.2
000400****************************************************************  IX1064.2
000500*                                                              *  IX1064.2
000600*    VALIDATION FOR:-                                          *  IX1064.2
000700*                                                              *  IX1064.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IX1064.2
000900*                                                              *  IX1064.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".IX1064.2
001100*                                                              *  IX1064.2
001200****************************************************************  IX1064.2
001300* THE PURPOSE OF THIS PROGRAM IS TO TEST THE ABILITY TO           IX1064.2
001400* USE THE THREE DIFFERENT TYPES OF FILES (SEQUENTIAL ,            IX1064.2
001500* INDEXED , AND RELATIVE) IN ONE PROGRAM . THE PROGRAM            IX1064.2
001600* IS BROKEN INTO FIVE SECTIONS . THE FIRST SECTION TESTS          IX1064.2
001700* THE ABILITY TO CREATE A RELATIVE FILE RANDOMLY . THE            IX1064.2
001800* SECOND SECTION TESTS THE ABILITY TO USE ALL THREE FILE          IX1064.2
001900* TYPES , READING IN THE RELATIVE FILE AND WRITING OUT THE        IX1064.2
002000* SEQUENTIAL AND INDEXED FILES . THE THIRD SECTION FURTHER        IX1064.2
002100* TESTS THE ABILITY TO USE THE THREE FILE TYPES . THE FOURTH      IX1064.2
002200* SECTION TESTS THE ABILITY TO DELETE RECORDS FROM THE            IX1064.2
002300* DIFFERENT FILE TYPES . THE FIFTH SECTION TESTS THE ABILITY      IX1064.2
002400* TO REWRITE RECORDS TO EACH OF THE FILE TYPES .                  IX1064.2
002500*                                                                 IX1064.2
002600* X-CARDS WHICH MUST BE RELACED WITH IMPLEMENTORS NAMES' IN       IX1064.2
002700* THIS PROGRAM ARE :                                              IX1064.2
002800*                                                                 IX1064.2
002900*        X-14 IMPLEMENTOR-NAME IN ASSIGN TO CLAUSE FOR            IX1064.2
003000*             SEQUENTIAL FILE .                                   IX1064.2
003100*        X-21 IMPLEMENTOR-NAME IN ASSIGN TO CLAUSE FOR            IX1064.2
003200*             RELATIVE FILE-1 , FILE-2 .                          IX1064.2
003300*        X-24 IMPLEMENTOR-NAME IN ASSIGN TO CLAUSE FOR            IX1064.2
003400*             INDEXED FILE .                                      IX1064.2
003500*        X-44 SYSTEM-NAME IN ASSIGN CLAUSE FOR                    IX1064.2
003600*        INDEXED FILE .                                           IX1064.2
003700*        X-55 SYSTEM PRINTER .                                    IX1064.2
003800*        X-62   FOR RAW-DATA                                      IX1064.2
003900*        X-82 SOURCE-COMPUTER .                                   IX1064.2
004000*        X-83 OBJECT-COMPUTER .                                   IX1064.2
004100*    C   X-84 PRINTE-FILE LABELS                                  IX1064.2
004200****************************************************************  IX1064.2
004300 ENVIRONMENT DIVISION.                                            IX1064.2
004400 CONFIGURATION SECTION.                                           IX1064.2
004500 SOURCE-COMPUTER.                                                 IX1064.2
004600     XXXXX082.                                                    IX1064.2
004700 OBJECT-COMPUTER.                                                 IX1064.2
004800     XXXXX083.                                                    IX1064.2
004900 INPUT-OUTPUT SECTION.                                            IX1064.2
005000 FILE-CONTROL.                                                    IX1064.2
005100*    SELECT RAW-DATA   ASSIGN TO                                  IX1064.2
005200*    XXXXX062                                                     IX1064.2
005300*           ORGANIZATION IS INDEXED                               IX1064.2
005400*           ACCESS MODE IS RANDOM                                 IX1064.2
005500*           RECORD KEY IS RAW-DATA-KEY.                           IX1064.2
005600     SELECT PRINT-FILE ASSIGN TO                                  IX1064.2
005700     XXXXX055.                                                    IX1064.2
005800     SELECT  RL-FR1                                               IX1064.2
005900     ASSIGN TO                                                    IX1064.2
006000     XXXXX021                                                     IX1064.2
006100        ACCESS MODE IS RANDOM                                     IX1064.2
006200        RELATIVE KEY IS RL-KEY                                    IX1064.2
006300        ORGANIZATION IS RELATIVE                                  IX1064.2
006400        FILE STATUS IS FR1-STATUS.                                IX1064.2
006500     SELECT  IX-FS1                                               IX1064.2
006600     ASSIGN TO                                                    IX1064.2
006700     XXXXX024                                                     IX1064.2
006800*    XXXXX044                                                     IX1064.2
006900        ACCESS MODE IS RANDOM                                     IX1064.2
007000        ORGANIZATION IS INDEXED                                   IX1064.2
007100        RECORD KEY IS IX-FS1-KEY-11-13                            IX1064.2
007200        FILE STATUS IS FS1-STATUS-IX.                             IX1064.2
007300     SELECT  SQ-FS1                                               IX1064.2
007400     ASSIGN TO                                                    IX1064.2
007500     XXXXX014                                                     IX1064.2
007600        ACCESS MODE IS SEQUENTIAL                                 IX1064.2
007700        ORGANIZATION IS SEQUENTIAL                                IX1064.2
007800        FILE STATUS IS FS1-STATUS-SQ.                             IX1064.2
007900 DATA DIVISION.                                                   IX1064.2
008000 FILE SECTION.                                                    IX1064.2
008100*                                                                 IX1064.2
008200*FD  RAW-DATA.                                                    IX1064.2
008300*                                                                 IX1064.2
008400*01  RAW-DATA-SATZ.                                               IX1064.2
008500*    05  RAW-DATA-KEY        PIC X(6).                            IX1064.2
008600*    05  C-DATE              PIC 9(6).                            IX1064.2
008700*    05  C-TIME              PIC 9(8).                            IX1064.2
008800*    05  C-NO-OF-TESTS       PIC 99.                              IX1064.2
008900*    05  C-OK                PIC 999.                             IX1064.2
009000*    05  C-ALL               PIC 999.                             IX1064.2
009100*    05  C-FAIL              PIC 999.                             IX1064.2
009200*    05  C-DELETED           PIC 999.                             IX1064.2
009300*    05  C-INSPECT           PIC 999.                             IX1064.2
009400*    05  C-NOTE              PIC X(13).                           IX1064.2
009500*    05  C-INDENT            PIC X.                               IX1064.2
009600*    05  C-ABORT             PIC X(8).                            IX1064.2
009700 FD  PRINT-FILE.                                                  IX1064.2
009800 01  PRINT-REC PICTURE X(120).                                    IX1064.2
009900 01  DUMMY-RECORD PICTURE X(120).                                 IX1064.2
010000 FD  RL-FR1                                                       IX1064.2
010100C    LABEL RECORDS ARE STANDARD                                   IX1064.2
010200C    DATA RECORD IS RL-FR1R1-F-G-241                              IX1064.2
010300     RECORD CONTAINS 241 CHARACTERS.                              IX1064.2
010400 01  RL-FR1R1-F-G-241.                                            IX1064.2
010500     05 RL-FR1-REC-120           PICTURE X(120).                  IX1064.2
010600     05 RL-FR1-REC-121-241.                                       IX1064.2
010700        10 FILLER                PICTURE X(8).                    IX1064.2
010800        10 RL-REC-KEY-AREA.                                       IX1064.2
010900           15 RL-FR1-KEY.                                         IX1064.2
011000              20 RL-FR1-KEY-1-10.                                 IX1064.2
011100                 25 RL-FR1-KEY-1-5      PICTURE X(5).             IX1064.2
011200                 25 RL-FR1-KEY-6-10     PICTURE X(5).             IX1064.2
011300               20 RL-FR1-KEY-11-13      PICTURE 9(3).             IX1064.2
011400           15 RL-REDF-RECKEY REDEFINES RL-FR1-KEY.                IX1064.2
011500              20 R-RECKEY-1-7    PICTURE X(7).                    IX1064.2
011600              20 R-RECKEY-8-13   PICTURE X(6).                    IX1064.2
011700           15 FILLER             PICTURE X(16).                   IX1064.2
011800        10 FILLER                PICTURE X(9).                    IX1064.2
011900        10 RL-ALT-KEY1-AREA.                                      IX1064.2
012000           15 RL-FR1-ALTKEY1.                                     IX1064.2
012100              20 RL-FR1-ALTKEY1-1-10.                             IX1064.2
012200                 25 RL-FR1-ALTKEY1-1-5  PICTURE X(5).             IX1064.2
012300                 25 RL-FR1-ALTKEY1-6-10 PICTURE X(5).             IX1064.2
012400              20 RL-FR1-ALTKEY1-11-13   PICTURE X(3).             IX1064.2
012500              20 RL-FR1-ALTKEY1-14-20   PICTURE X(7).             IX1064.2
012600           15 RL-REDF-ALTKEY1 REDEFINES RL-FR1-ALTKEY1.           IX1064.2
012700              20 R-ALTKEY1-1-6   PICTURE X(6).                    IX1064.2
012800              20 R-ALTKEY1-7-10  PICTURE X(4).                    IX1064.2
012900              20 R-ALTKEY1-11-20 PICTURE X(10).                   IX1064.2
013000           15 FILLER             PICTURE X(9).                    IX1064.2
013100        10 FILLER                PICTURE X(9).                    IX1064.2
013200        10 RL-ALT-KEY2-AREA.                                      IX1064.2
013300           15 RL-FR1-ALTKEY2.                                     IX1064.2
013400              20 RL-FR1-ALTKEY2-1-10.                             IX1064.2
013500                 25 RL-FR1-ALTKEY2-1-5  PICTURE X(5).             IX1064.2
013600                 25 RL-FR1-ALTKEY2-6-10 PICTURE X(5).             IX1064.2
013700              20 RL-FR1-ALTKEY2-11-13   PICTURE X(3).             IX1064.2
013800              20 RL-FR1-ALTKEY2-14-20   PICTURE X(7).             IX1064.2
013900           15 FILLER             PICTURE X(9).                    IX1064.2
014000        10 FILLER                PICTURE X(8).                    IX1064.2
014100 FD  IX-FS1                                                       IX1064.2
014200C    LABEL RECORDS ARE STANDARD                                   IX1064.2
014300C    DATA RECORD IS IX-FS1R1-F-G-241                              IX1064.2
014400     RECORD CONTAINS 241 CHARACTERS.                              IX1064.2
014500 01  IX-FS1R1-F-G-241.                                            IX1064.2
014600     05 IX-FS1-REC-120           PICTURE X(120).                  IX1064.2
014700     05 IX-FS1-REC-121-241.                                       IX1064.2
014800        10 FILLER                PICTURE X(8).                    IX1064.2
014900        10 IX-REC-KEY-AREA.                                       IX1064.2
015000           15 IX-FS1-KEY.                                         IX1064.2
015100              20 IX-FS1-KEY-1-10.                                 IX1064.2
015200                 25 IX-FS1-KEY-1-5      PICTURE X(5).             IX1064.2
015300                 25 IX-FS1-KEY-6-10     PICTURE X(5).             IX1064.2
015400              20 IX-FS1-KEY-11-13      PICTURE X(3).              IX1064.2
015500           15 IX-REDF-RECKEY REDEFINES IX-FS1-KEY.                IX1064.2
015600              20 I-RECKEY-1-7    PICTURE X(7).                    IX1064.2
015700              20 I-RECKEY-8-13   PICTURE X(6).                    IX1064.2
015800           15 FILLER             PICTURE X(16).                   IX1064.2
015900        10 FILLER                PICTURE X(9).                    IX1064.2
016000        10 IX-ALT-KEY1-AREA.                                      IX1064.2
016100           15 IX-FS1-ALTKEY1.                                     IX1064.2
016200              20 IX-FS1-ALTKEY1-1-10.                             IX1064.2
016300                 25 IX-FS1-ALTKEY1-1-5  PICTURE X(5).             IX1064.2
016400                 25 IX-FS1-ALTKEY1-6-10 PICTURE X(5).             IX1064.2
016500              20 IX-FS1-ALTKEY1-11-13   PICTURE X(3).             IX1064.2
016600              20 IX-FS1-ALTKEY1-14-20   PICTURE X(7).             IX1064.2
016700           15 IX-REDF-ALTKEY1 REDEFINES IX-FS1-ALTKEY1.           IX1064.2
016800              20 I-ALTKEY1-1-6   PICTURE X(6).                    IX1064.2
016900              20 I-ALTKEY1-7-10  PICTURE X(4).                    IX1064.2
017000              20 I-ALTKEY1-11-20 PICTURE X(10).                   IX1064.2
017100           15 FILLER             PICTURE X(9).                    IX1064.2
017200        10 FILLER                PICTURE X(9).                    IX1064.2
017300        10 IX-ALT-KEY2-AREA.                                      IX1064.2
017400           15 IX-FS1-ALTKEY2.                                     IX1064.2
017500              20 IX-FS1-ALTKEY2-1-10.                             IX1064.2
017600                 25 IX-FR1-ALTKEY2-1-5  PICTURE X(5).             IX1064.2
017700                 25 IX-FR1-ALTKEY2-6-10 PICTURE X(5).             IX1064.2
017800              20 IX-FS1-ALTKEY2-11-13   PICTURE X(3).             IX1064.2
017900              20 IX-FS1-ALTKEY2-14-20   PICTURE X(7).             IX1064.2
018000           15 FILLER             PICTURE X(9).                    IX1064.2
018100        10 FILLER                PICTURE X(8).                    IX1064.2
018200 FD  SQ-FS1                                                       IX1064.2
018300C    LABEL RECORDS ARE STANDARD                                   IX1064.2
018400C    DATA RECORD IS SQ-FS1R1-F-G-241                              IX1064.2
018500     RECORD CONTAINS 241 CHARACTERS.                              IX1064.2
018600 01  SQ-FS1R1-F-G-241.                                            IX1064.2
018700     05 SQ-FS1-REC-120           PICTURE X(120).                  IX1064.2
018800     05 SQ-FS1-REC-121-241.                                       IX1064.2
018900        10 FILLER                PICTURE X(8).                    IX1064.2
019000        10 SQ-REC-KEY-AREA.                                       IX1064.2
019100           15 SQ-FS1-KEY.                                         IX1064.2
019200              20 SQ-FS1-KEY-1-10.                                 IX1064.2
019300                 25 SQ-FS1-KEY-1-5      PICTURE X(5).             IX1064.2
019400                 25 SQ-FS1-KEY-6-10     PICTURE X(5).             IX1064.2
019500               20 SQ-FS1-KEY-11-13      PICTURE 9(3).             IX1064.2
019600           15 SQ-REDF-RECKEY REDEFINES SQ-FS1-KEY.                IX1064.2
019700              20 S-RECKEY-1-7    PICTURE X(7).                    IX1064.2
019800              20 S-RECKEY-8-13   PICTURE X(6).                    IX1064.2
019900           15 FILLER             PICTURE X(16).                   IX1064.2
020000        10 FILLER                PICTURE X(9).                    IX1064.2
020100        10 SQ-ALT-KEY1-AREA.                                      IX1064.2
020200           15 SQ-FS1-ALTKEY1.                                     IX1064.2
020300              20 SQ-FS1-ALTKEY1-1-10.                             IX1064.2
020400                 25 SQ-FS1-ALTKEY1-1-5  PICTURE X(5).             IX1064.2
020500                 25 SQ-FS1-ALTKEY1-6-10 PICTURE X(5).             IX1064.2
020600              20 SQ-FS1-ALTKEY1-11-13   PICTURE X(3).             IX1064.2
020700              20 SQ-FS1-ALTKEY1-14-20   PICTURE X(7).             IX1064.2
020800           15 SQ-REDF-ALTKEY1 REDEFINES SQ-FS1-ALTKEY1.           IX1064.2
020900              20 S-ALTKEY1-1-6   PICTURE X(6).                    IX1064.2
021000              20 S-ALTKEY1-7-10  PICTURE X(4).                    IX1064.2
021100              20 S-ALTKEY1-11-20 PICTURE X(10).                   IX1064.2
021200           15 FILLER             PICTURE X(9).                    IX1064.2
021300        10 FILLER                PICTURE X(9).                    IX1064.2
021400        10 SQ-ALT-KEY2-AREA.                                      IX1064.2
021500           15 SQ-FS1-ALTKEY2.                                     IX1064.2
021600              20 SQ-FS1-ALTKEY2-1-10.                             IX1064.2
021700                 25 SQ-FS1-ALTKEY2-1-5  PICTURE X(5).             IX1064.2
021800                 25 SQ-FS1-ALTKEY2-6-10 PICTURE X(5).             IX1064.2
021900              20 SQ-FS1-ALTKEY2-11-13   PICTURE X(3).             IX1064.2
022000              20 SQ-FS1-ALTKEY2-14-20   PICTURE X(7).             IX1064.2
022100           15 FILLER             PICTURE X(9).                    IX1064.2
022200        10 FILLER                PICTURE X(8).                    IX1064.2
022300 WORKING-STORAGE SECTION.                                         IX1064.2
022400 01  WRK-DS-09V00-001 PIC S9(09)      USAGE COMP VALUE ZERO.      IX1064.2
022500 01  WRK-DS-09V00-002 PIC 9(3) VALUE ZERO.                        IX1064.2
022600 01  WRK-CS-09V00-002 PIC S9(09)       USAGE COMP VALUE ZERO.     IX1064.2
022700 01  WRK-CS-09V00-003 PIC S9(09)       USAGE COMP VALUE ZERO.     IX1064.2
022800 01  RL-KEY   PIC 9(3).                                           IX1064.2
022900 01  RL-KEY2  PIC 9(3).                                           IX1064.2
023000*                                                                 IX1064.2
023100 01  WRK-FR1-RECKEY.                                              IX1064.2
023200     05 FR1-RECKEY-1-13.                                          IX1064.2
023300        10 FR1-RECKEY-1-10       PICTURE X(10).                   IX1064.2
023400        10 FR1-RECKEY-11-13      PICTURE 9(3).                    IX1064.2
023500     05 FILLER                   PICTURE X(16)  VALUE SPACE.      IX1064.2
023600 01  WRK-FR1-ALTKEY1.                                             IX1064.2
023700      05 FR1-ALTKEY1-1-20.                                        IX1064.2
023800       10  FR1-ALTKEY1-1-10.                                      IX1064.2
023900        15 FR1-ALTKEY1-1-5       PICTURE X(5).                    IX1064.2
024000        15 FR1-ALTKEY1-6-10      PICTURE X(5).                    IX1064.2
024100       10 FR1-ALTKEY1-11-13      PICTURE 9(3).                    IX1064.2
024200       10 FR1-ALTKEY1-14-20      PICTURE X(7).                    IX1064.2
024300     05 FILLER                   PICTURE X(9)  VALUE SPACE.       IX1064.2
024400 01  WRK-FR1-ALTKEY2.                                             IX1064.2
024500     05 FR1-ALTKEY2-1-20.                                         IX1064.2
024600        10 FR1-ALTKEY2-1-10.                                      IX1064.2
024700           15 FR1-ALTKEY2-1-5    PICTURE X(5).                    IX1064.2
024800           15 FR1-ALTKEY2-6-10   PICTURE X(5).                    IX1064.2
024900        10 FR1-ALTKEY2-11-13     PICTURE 9(3).                    IX1064.2
025000        10 FR1-ALTKEY2-14-20     PICTURE X(7).                    IX1064.2
025100     05 FILLER                   PICTURE X(9) VALUE SPACE.        IX1064.2
025200 01  RECNO                       PICTURE 9(5)  VALUE ZERO.        IX1064.2
025300 01  FR1-STATUS                  PICTURE XX   VALUE SPACE.        IX1064.2
025400 01  FS1-STATUS-IX PIC XX VALUE SPACE.                            IX1064.2
025500 01  FS1-STATUS-SQ PIC XX VALUE SPACE.                            IX1064.2
025600 01  EXCUT-COUNTER-06V00         PICTURE 9(6) VALUE ZERO.         IX1064.2
025700 01  KEYSUB                      PICTURE 9(3) COMPUTATIONAL.      IX1064.2
025800 01  INVKEY-COUNTER-RL           PICTURE 9(3) COMPUTATIONAL.      IX1064.2
025900 01  INVKEY-COUNTER-IX           PICTURE 9(3) COMPUTATIONAL.      IX1064.2
026000 01  RECORDS-WRITTEN             PICTURE 9(3).                    IX1064.2
026100 01  RECKEY-NUM                  PICTURE 9(3).                    IX1064.2
026200 01  ALTKEY1-NUM                 PICTURE 9(3).                    IX1064.2
026300 01  ALTKEY2-NUM                 PICTURE 9(3).                    IX1064.2
026400 01  RECORD-KEY-CONTENT.                                          IX1064.2
026500     05 FILLER PIC X(53) VALUE                                    IX1064.2
026600          "BBBBBBBBBC225EEEEEEEEEF001ALTKEY1WWWWWWWWWV076ALTKEY2".IX1064.2
026700     05 FILLER PIC X(53) VALUE                                    IX1064.2
026800          "BBBBBBBBCC224EEEEEEEEFF002ALTKEY1WWWWWWWWVV077ALTKEY2".IX1064.2
026900     05 FILLER PIC X(53) VALUE                                    IX1064.2
027000          "BBBBBBBCCC223EEEEEEEFFF003ALTKEY1WWWWWWWVVV078ALTKEY2".IX1064.2
027100     05 FILLER PIC X(53) VALUE                                    IX1064.2
027200          "BBBBBBCCCC222EEEEEEFFFF004ALTKEY1WWWWWWVVVV079ALTKEY2".IX1064.2
027300     05 FILLER PIC X(53) VALUE                                    IX1064.2
027400          "BBBBBCCCCC221EEEEEFFFFF005ALTKEY1WWWWWVVVVV080ALTKEY2".IX1064.2
027500     05 FILLER PIC X(53) VALUE                                    IX1064.2
027600          "BBBBCCCCCC220EEEEFFFFFF006ALTKEY1WWWWVVVVVV081ALTKEY2".IX1064.2
027700     05 FILLER PIC X(53) VALUE                                    IX1064.2
027800          "BBBCCCCCCC219EEEFFFFFFF007ALTKEY1WWWVVVVVVV082ALTKEY2".IX1064.2
027900     05 FILLER PIC X(53) VALUE                                    IX1064.2
028000          "BBCCCCCCCC218EEFFFFFFFF008ALTKEY1WWVVVVVVVV083ALTKEY2".IX1064.2
028100     05 FILLER PIC X(53) VALUE                                    IX1064.2
028200          "BCCCCCCCCC217EFFFFFFFFF009ALTKEY1WVVVVVVVVV084ALTKEY2".IX1064.2
028300     05 FILLER PIC X(53) VALUE                                    IX1064.2
028400          "CCCCCCCCCC216FFFFFFFFFF010ALTKEY1VVVVVVVVVV085ALTKEY2".IX1064.2
028500     05 FILLER PIC X(53) VALUE                                    IX1064.2
028600          "CCCCCCCCCD215FFFFFFFFFG011ALTKEY1VVVVVVVVVV086ALTKEY2".IX1064.2
028700     05 FILLER PIC X(53) VALUE                                    IX1064.2
028800          "CCCCCCCCDD214FFFFFFFFGG012ALTKEY1VVVVVVVVUU087ALTKEY2".IX1064.2
028900     05 FILLER PIC X(53) VALUE                                    IX1064.2
029000          "CCCCCCCDDD213FFFFFFFGGG013ALTKEY1VVVVVVVUUU088ALTKEY2".IX1064.2
029100     05 FILLER PIC X(53) VALUE                                    IX1064.2
029200          "CCCCCCDDDD212FFFFFFGGGG014ALTKEY1VVVVVVUUUU089ALTKEY2".IX1064.2
029300     05 FILLER PIC X(53) VALUE                                    IX1064.2
029400          "CCCCCDDDDD211FFFFFGGGGG015ALTKEY1VVVVVUUUUU090ALTKEY2".IX1064.2
029500     05 FILLER PIC X(53) VALUE                                    IX1064.2
029600          "CCCCDDDDDD210FFFFGGGGGG016ALTKEY1VVVVUUUUUU091ALTKEY2".IX1064.2
029700     05 FILLER PIC X(53) VALUE                                    IX1064.2
029800          "CCCDDDDDDD209FFFGGGGGGG017ALTKEY1VVVUUUUUUU092ALTKEY2".IX1064.2
029900     05 FILLER PIC X(53) VALUE                                    IX1064.2
030000          "CCDDDDDDDD208FFGGGGGGGG018ALTKEY1VVUUUUUUUU093ALTKEY2".IX1064.2
030100     05 FILLER PIC X(53) VALUE                                    IX1064.2
030200          "CDDDDDDDDD207FGGGGGGGGG019ALTKEY1VUUUUUUUUU094ALTKEY2".IX1064.2
030300     05 FILLER PIC X(53) VALUE                                    IX1064.2
030400          "DDDDDDDDDD206GGGGGGGGGG020ALTKEY1UUUUUUUUUU095ALTKEY2".IX1064.2
030500     05 FILLER PIC X(53) VALUE                                    IX1064.2
030600          "DDDDDDDDDE205GGGGGGGGGH021ALTKEY1UUUUUUUUUU096ALTKEY2".IX1064.2
030700     05 FILLER PIC X(53) VALUE                                    IX1064.2
030800          "DDDDDDDDEE204GGGGGGGGHH022ALTKEY1UUUUUUUUTT097ALTKEY2".IX1064.2
030900     05 FILLER PIC X(53) VALUE                                    IX1064.2
031000          "DDDDDDDEEE203GGGGGGGHHH023ALTKEY1UUUUUUUTTT098ALTKEY2".IX1064.2
031100     05 FILLER PIC X(53) VALUE                                    IX1064.2
031200          "DDDDDDEEEE202GGGGGGHHHH024ALTKEY1UUUUUUTTTT099ALTKEY2".IX1064.2
031300     05 FILLER PIC X(53) VALUE                                    IX1064.2
031400          "DDDDDEEEEE201GGGGGHHHHH025ALTKEY1UUUUUTTTTT100ALTKEY2".IX1064.2
031500     05 FILLER PIC X(53) VALUE                                    IX1064.2
031600          "DDDDEEEEEE200GGGGHHHHHH026ALTKEY1UUUUTTTTTT101ALTKEY2".IX1064.2
031700     05 FILLER PIC X(53) VALUE                                    IX1064.2
031800          "DDDEEEEEEE199GGGHHHHHHH027ALTKEY1UUUTTTTTTT102ALTKEY2".IX1064.2
031900     05 FILLER PIC X(53) VALUE                                    IX1064.2
032000          "DDEEEEEEEE198GGHHHHHHHH028ALTKEY1UUTTTTTTTT103ALTKEY2".IX1064.2
032100     05 FILLER PIC X(53) VALUE                                    IX1064.2
032200          "DEEEEEEEEE197GHHHHHHHHH029ALTKEY1UTTTTTTTTT104ALTKEY2".IX1064.2
032300     05 FILLER PIC X(53) VALUE                                    IX1064.2
032400          "EEEEEEEEEE196HHHHHHHHHH030ALTKEY1TTTTTTTTTT105ALTKEY2".IX1064.2
032500     05 FILLER PIC X(53) VALUE                                    IX1064.2
032600          "EEEEEEEEEF195HHHHHHHHHI031ALTKEY1TTTTTTTTTT106ALTKEY2".IX1064.2
032700     05 FILLER PIC X(53) VALUE                                    IX1064.2
032800          "EEEEEEEEFF194HHHHHHHHII032ALTKEY1TTTTTTTTSS107ALTKEY2".IX1064.2
032900     05 FILLER PIC X(53) VALUE                                    IX1064.2
033000          "EEEEEEEFFF193HHHHHHHIII033ALTKEY1TTTTTTTSSS108ALTKEY2".IX1064.2
033100     05 FILLER PIC X(53) VALUE                                    IX1064.2
033200          "EEEEEEFFFF192HHHHHHIIII034ALTKEY1TTTTTTSSSS109ALTKEY2".IX1064.2
033300     05 FILLER PIC X(53) VALUE                                    IX1064.2
033400          "EEEEEFFFFF191HHHHHIIIII035ALTKEY1TTTTTSSSSS110ALTKEY2".IX1064.2
033500     05 FILLER PIC X(53) VALUE                                    IX1064.2
033600          "EEEEFFFFFF190HHHHIIIIII036ALTKEY1TTTTSSSSSS111ALTKEY2".IX1064.2
033700     05 FILLER PIC X(53) VALUE                                    IX1064.2
033800          "EEEFFFFFFF189HHHIIIIIII037ALTKEY1TTTSSSSSSS112ALTKEY2".IX1064.2
033900     05 FILLER PIC X(53) VALUE                                    IX1064.2
034000          "EEFFFFFFFF188HHIIIIIIII038ALTKEY1TTSSSSSSSS113ALTKEY2".IX1064.2
034100     05 FILLER PIC X(53) VALUE                                    IX1064.2
034200          "EFFFFFFFFF187HIIIIIIIII039ALTKEY1TSSSSSSSSS114ALTKEY2".IX1064.2
034300     05 FILLER PIC X(53) VALUE                                    IX1064.2
034400          "FFFFFFFFFF186IIIIIIIIII040ALTKEY1SSSSSSSSSS115ALTKEY2".IX1064.2
034500     05 FILLER PIC X(53) VALUE                                    IX1064.2
034600          "FFFFFFFFFG185IIIIIIIIIJ041ALTKEY1SSSSSSSSSS116ALTKEY2".IX1064.2
034700     05 FILLER PIC X(53) VALUE                                    IX1064.2
034800          "FFFFFFFFGG184IIIIIIIIJJ042ALTKEY1SSSSSSSSRR117ALTKEY2".IX1064.2
034900     05 FILLER PIC X(53) VALUE                                    IX1064.2
035000          "FFFFFFFGGG183IIIIIIIJJJ043ALTKEY1SSSSSSSRRR118ALTKEY2".IX1064.2
035100     05 FILLER PIC X(53) VALUE                                    IX1064.2
035200          "FFFFFFGGGG182IIIIIIJJJJ044ALTKEY1SSSSSSRRRR119ALTKEY2".IX1064.2
035300     05 FILLER PIC X(53) VALUE                                    IX1064.2
035400          "FFFFFGGGGG181IIIIIJJJJJ045ALTKEY1SSSSSRRRRR120ALTKEY2".IX1064.2
035500     05 FILLER PIC X(53) VALUE                                    IX1064.2
035600          "FFFFGGGGGG180IIIIJJJJJJ046ALTKEY1SSSSRRRRRR121ALTKEY2".IX1064.2
035700     05 FILLER PIC X(53) VALUE                                    IX1064.2
035800          "FFFGGGGGGG179IIIJJJJJJJ047ALTKEY1SSSRRRRRRR122ALTKEY2".IX1064.2
035900     05 FILLER PIC X(53) VALUE                                    IX1064.2
036000          "FFGGGGGGGG178IIJJJJJJJJ048ALTKEY1SSRRRRRRRR123ALTKEY2".IX1064.2
036100     05 FILLER PIC X(53) VALUE                                    IX1064.2
036200          "FGGGGGGGGG177IJJJJJJJJJ049ALTKEY1SRRRRRRRRR124ALTKEY2".IX1064.2
036300     05 FILLER PIC X(53) VALUE                                    IX1064.2
036400          "GGGGGGGGGG176JJJJJJJJJJ050ALTKEY1RRRRRRRRRR125ALTKEY2".IX1064.2
036500     05 FILLER PIC X(53) VALUE                                    IX1064.2
036600          "RRRRSSSSSS175VVVVWWWWWW051ALTKEY1GGGGFFFFFF126ALTKEY2".IX1064.2
036700     05 FILLER PIC X(53) VALUE                                    IX1064.2
036800          "RRRSSSSSSS174VVVWWWWWWW052ALTKEY1GGGFFFFFFF127ALTKEY2".IX1064.2
036900     05 FILLER PIC X(53) VALUE                                    IX1064.2
037000          "RRSSSSSSSS173VVWWWWWWWW053ALTKEY1GGFFFFFFFF128ALTKEY2".IX1064.2
037100     05 FILLER PIC X(53) VALUE                                    IX1064.2
037200          "RSSSSSSSSS172VWWWWWWWWW054ALTKEY1GFFFFFFFFF129ALTKEY2".IX1064.2
037300     05 FILLER PIC X(53) VALUE                                    IX1064.2
037400          "SSSSSSSSSS171WWWWWWWWWW055ALTKEY1FFFFFFFFFF130ALTKEY2".IX1064.2
037500     05 FILLER PIC X(53) VALUE                                    IX1064.2
037600          "SSSSSSSSST170WWWWWWWWWX056ALTKEY1FFFFFFFFFF131ALTKEY2".IX1064.2
037700     05 FILLER PIC X(53) VALUE                                    IX1064.2
037800          "SSSSSSSSTT169WWWWWWWWXX057ALTKEY1FFFFFFFFEE132ALTKEY2".IX1064.2
037900     05 FILLER PIC X(53) VALUE                                    IX1064.2
038000          "SSSSSSSTTT168WWWWWWWXXX058ALTKEY1FFFFFFFEEE133ALTKEY2".IX1064.2
038100     05 FILLER PIC X(53) VALUE                                    IX1064.2
038200          "SSSSSSTTTT167WWWWWWXXXX059ALTKEY1FFFFFFEEEE134ALTKEY2".IX1064.2
038300     05 FILLER PIC X(53) VALUE                                    IX1064.2
038400          "SSSSSTTTTT166WWWWWXXXXX060ALTKEY1FFFFFEEEEE135ALTKEY2".IX1064.2
038500     05 FILLER PIC X(53) VALUE                                    IX1064.2
038600          "SSSSTTTTTT165WWWWXXXXXX061ALTKEY1FFFFEEEEEE136ALTKEY2".IX1064.2
038700     05 FILLER PIC X(53) VALUE                                    IX1064.2
038800          "SSSTTTTTTT164WWWXXXXXXX062ALTKEY1FFFEEEEEEE137ALTKEY2".IX1064.2
038900     05 FILLER PIC X(53) VALUE                                    IX1064.2
039000          "SSTTTTTTTT163WWXXXXXXXX063ALTKEY1FFEEEEEEEE138ALTKEY2".IX1064.2
039100     05 FILLER PIC X(53) VALUE                                    IX1064.2
039200          "STTTTTTTTT162WXXXXXXXXX064ALTKEY1FEEEEEEEEE139ALTKEY2".IX1064.2
039300     05 FILLER PIC X(53) VALUE                                    IX1064.2
039400          "TTTTTTTTTT161XXXXXXXXXX065ALTKEY1EEEEEEEEEE140ALTKEY2".IX1064.2
039500     05 FILLER PIC X(53) VALUE                                    IX1064.2
039600          "TTTTTTTTTU160XXXXXXXXXY066ALTKEY1EEEEEEEEEE141ALTKEY2".IX1064.2
039700     05 FILLER PIC X(53) VALUE                                    IX1064.2
039800          "TTTTTTTTUU159XXXXXXXXYY067ALTKEY1EEEEEEEEDD142ALTKEY2".IX1064.2
039900     05 FILLER PIC X(53) VALUE                                    IX1064.2
040000          "TTTTTTTUUU158XXXXXXXYYY068ALTKEY1EEEEEEEDDD143ALTKEY2".IX1064.2
040100     05 FILLER PIC X(53) VALUE                                    IX1064.2
040200          "TTTTTTUUUU157XXXXXXYYYY069ALTKEY1EEEEEEDDDD144ALTKEY2".IX1064.2
040300     05 FILLER PIC X(53) VALUE                                    IX1064.2
040400          "TTTTTUUUUU156XXXXXYYYYY070ALTKEY1EEEEEDDDDD145ALTKEY2".IX1064.2
040500     05 FILLER PIC X(53) VALUE                                    IX1064.2
040600          "TTTTUUUUUU155XXXXYYYYYY071ALTKEY1EEEEDDDDDD146ALTKEY2".IX1064.2
040700     05 FILLER PIC X(53) VALUE                                    IX1064.2
040800          "TTTUUUUUUU154XXXYYYYYYY072ALTKEY1EEEDDDDDDD147ALTKEY2".IX1064.2
040900     05 FILLER PIC X(53) VALUE                                    IX1064.2
041000          "TTUUUUUUUU153XXYYYYYYYY073ALTKEY1EEDDDDDDDD148ALTKEY2".IX1064.2
041100     05 FILLER PIC X(53) VALUE                                    IX1064.2
041200          "TUUUUUUUUU152XYYYYYYYYY074ALTKEY1EDDDDDDDDD149ALTKEY2".IX1064.2
041300     05 FILLER PIC X(53) VALUE                                    IX1064.2
041400          "UUUUUUUUUU151YYYYYYYYYY075ALTKEY1DDDDDDDDDD150ALTKEY2".IX1064.2
041500 01  RECORD-KEY-DATA   REDEFINES  RECORD-KEY-CONTENT.             IX1064.2
041600     05 KEY-VALUES                  OCCURS 75  TIMES.             IX1064.2
041700       10 RECKEY-VALUE           PICTURE X(13).                   IX1064.2
041800       10 ALTKEY1-VALUE          PICTURE X(20).                   IX1064.2
041900       10 ALTKEY2-VALUE          PICTURE X(20).                   IX1064.2
042000 01  INIT-FLAG                   PICTURE 9.                       IX1064.2
042100 01  HOLD-FILESTATUS-RECORD.                                      IX1064.2
042200     05 FILESTATUS               PICTURE XX  OCCURS 10 TIMES.     IX1064.2
042300                                                                  IX1064.2
042400 01  FILE-RECORD-INFORMATION-REC.                                 IX1064.2
042500     03 FILE-RECORD-INFO-SKELETON.                                IX1064.2
042600        05 FILLER                 PICTURE X(48)       VALUE       IX1064.2
042700             "FILE=      ,RECORD=      /0,RECNO=000000,UPDT=00".  IX1064.2
042800        05 FILLER                 PICTURE X(46)       VALUE       IX1064.2
042900             ",ODO=0000,PGM=     ,LRECL=000000,BLKSIZ  =0000".    IX1064.2
043000        05 FILLER                 PICTURE X(26)       VALUE       IX1064.2
043100             ",LFIL=000000,ORG=  ,LBLR= ".                        IX1064.2
043200        05 FILLER                 PICTURE X(37)       VALUE       IX1064.2
043300             ",RECKEY=                             ".             IX1064.2
043400        05 FILLER                 PICTURE X(38)       VALUE       IX1064.2
043500             ",ALTKEY1=                             ".            IX1064.2
043600        05 FILLER                 PICTURE X(38)       VALUE       IX1064.2
043700             ",ALTKEY2=                             ".            IX1064.2
043800        05 FILLER                 PICTURE X(7)        VALUE SPACE.IX1064.2
043900     03 FILE-RECORD-INFO          OCCURS  10  TIMES.              IX1064.2
044000        05 FILE-RECORD-INFO-P1-120.                               IX1064.2
044100           07 FILLER              PIC X(5).                       IX1064.2
044200           07 XFILE-NAME           PIC X(6).                      IX1064.2
044300           07 FILLER              PIC X(8).                       IX1064.2
044400           07 XRECORD-NAME         PIC X(6).                      IX1064.2
044500           07 FILLER              PIC X(1).                       IX1064.2
044600           07 REELUNIT-NUMBER     PIC 9(1).                       IX1064.2
044700           07 FILLER              PIC X(7).                       IX1064.2
044800           07 XRECORD-NUMBER       PIC 9(6).                      IX1064.2
044900           07 FILLER              PIC X(6).                       IX1064.2
045000           07 UPDATE-NUMBER       PIC 9(2).                       IX1064.2
045100           07 FILLER              PIC X(5).                       IX1064.2
045200           07 ODO-NUMBER          PIC 9(4).                       IX1064.2
045300           07 FILLER              PIC X(5).                       IX1064.2
045400           07 XPROGRAM-NAME        PIC X(5).                      IX1064.2
045500           07 FILLER              PIC X(7).                       IX1064.2
045600           07 XRECORD-LENGTH       PIC 9(6).                      IX1064.2
045700           07 FILLER              PIC X(7).                       IX1064.2
045800           07 CHARS-OR-RECORDS    PIC X(2).                       IX1064.2
045900           07 FILLER              PIC X(1).                       IX1064.2
046000           07 XBLOCK-SIZE          PIC 9(4).                      IX1064.2
046100           07 FILLER              PIC X(6).                       IX1064.2
046200           07 RECORDS-IN-FILE     PIC 9(6).                       IX1064.2
046300           07 FILLER              PIC X(5).                       IX1064.2
046400           07 XFILE-ORGANIZATION   PIC X(2).                      IX1064.2
046500           07 FILLER              PIC X(6).                       IX1064.2
046600           07 XLABEL-TYPE          PIC X(1).                      IX1064.2
046700        05 FILE-RECORD-INFO-P121-240.                             IX1064.2
046800           07 FILLER              PIC X(8).                       IX1064.2
046900           07 XRECORD-KEY          PIC X(29).                     IX1064.2
047000           07 FILLER              PIC X(9).                       IX1064.2
047100           07 ALTERNATE-KEY1      PIC X(29).                      IX1064.2
047200           07 FILLER              PIC X(9).                       IX1064.2
047300           07 ALTERNATE-KEY2      PIC X(29).                      IX1064.2
047400           07 FILLER              PIC X(7).                       IX1064.2
047500                                                                  IX1064.2
047600 01  TEST-RESULTS.                                                IX1064.2
047700     02 FILLER                   PIC X      VALUE SPACE.          IX1064.2
047800     02 FEATURE                  PIC X(20)  VALUE SPACE.          IX1064.2
047900     02 FILLER                   PIC X      VALUE SPACE.          IX1064.2
048000     02 P-OR-F                   PIC X(5)   VALUE SPACE.          IX1064.2
048100     02 FILLER                   PIC X      VALUE SPACE.          IX1064.2
048200     02  PAR-NAME.                                                IX1064.2
048300       03 FILLER                 PIC X(19)  VALUE SPACE.          IX1064.2
048400       03  PARDOT-X              PIC X      VALUE SPACE.          IX1064.2
048500       03 DOTVALUE               PIC 99     VALUE ZERO.           IX1064.2
048600     02 FILLER                   PIC X(8)   VALUE SPACE.          IX1064.2
048700     02 RE-MARK                  PIC X(61).                       IX1064.2
048800 01  TEST-COMPUTED.                                               IX1064.2
048900     02 FILLER                   PIC X(30)  VALUE SPACE.          IX1064.2
049000     02 FILLER                   PIC X(17)  VALUE                 IX1064.2
049100            "       COMPUTED=".                                   IX1064.2
049200     02 COMPUTED-X.                                               IX1064.2
049300     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          IX1064.2
049400     03 COMPUTED-N               REDEFINES COMPUTED-A             IX1064.2
049500                                 PIC -9(9).9(9).                  IX1064.2
049600     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         IX1064.2
049700     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     IX1064.2
049800     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     IX1064.2
049900     03       CM-18V0 REDEFINES COMPUTED-A.                       IX1064.2
050000         04 COMPUTED-18V0                    PIC -9(18).          IX1064.2
050100         04 FILLER                           PIC X.               IX1064.2
050200     03 FILLER PIC X(50) VALUE SPACE.                             IX1064.2
050300 01  TEST-CORRECT.                                                IX1064.2
050400     02 FILLER PIC X(30) VALUE SPACE.                             IX1064.2
050500     02 FILLER PIC X(17) VALUE "       CORRECT =".                IX1064.2
050600     02 CORRECT-X.                                                IX1064.2
050700     03 CORRECT-A                  PIC X(20) VALUE SPACE.         IX1064.2
050800     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      IX1064.2
050900     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         IX1064.2
051000     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     IX1064.2
051100     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     IX1064.2
051200     03      CR-18V0 REDEFINES CORRECT-A.                         IX1064.2
051300         04 CORRECT-18V0                     PIC -9(18).          IX1064.2
051400         04 FILLER                           PIC X.               IX1064.2
051500     03 FILLER PIC X(2) VALUE SPACE.                              IX1064.2
051600     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     IX1064.2
051700 01  CCVS-C-1.                                                    IX1064.2
051800     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PAIX1064.2
051900-    "SS  PARAGRAPH-NAME                                          IX1064.2
052000-    "       REMARKS".                                            IX1064.2
052100     02 FILLER                     PIC X(20)    VALUE SPACE.      IX1064.2
052200 01  CCVS-C-2.                                                    IX1064.2
052300     02 FILLER                     PIC X        VALUE SPACE.      IX1064.2
052400     02 FILLER                     PIC X(6)     VALUE "TESTED".   IX1064.2
052500     02 FILLER                     PIC X(15)    VALUE SPACE.      IX1064.2
052600     02 FILLER                     PIC X(4)     VALUE "FAIL".     IX1064.2
052700     02 FILLER                     PIC X(94)    VALUE SPACE.      IX1064.2
052800 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       IX1064.2
052900 01  REC-CT                        PIC 99       VALUE ZERO.       IX1064.2
053000 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       IX1064.2
053100 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       IX1064.2
053200 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       IX1064.2
053300 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       IX1064.2
053400 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       IX1064.2
053500 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       IX1064.2
053600 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      IX1064.2
053700 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       IX1064.2
053800 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     IX1064.2
053900 01  CCVS-H-1.                                                    IX1064.2
054000     02  FILLER                    PIC X(39)    VALUE SPACES.     IX1064.2
054100     02  FILLER                    PIC X(42)    VALUE             IX1064.2
054200     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 IX1064.2
054300     02  FILLER                    PIC X(39)    VALUE SPACES.     IX1064.2
054400 01  CCVS-H-2A.                                                   IX1064.2
054500   02  FILLER                        PIC X(40)  VALUE SPACE.      IX1064.2
054600   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  IX1064.2
054700   02  FILLER                        PIC XXXX   VALUE             IX1064.2
054800     "4.2 ".                                                      IX1064.2
054900   02  FILLER                        PIC X(28)  VALUE             IX1064.2
055000            " COPY - NOT FOR DISTRIBUTION".                       IX1064.2
055100   02  FILLER                        PIC X(41)  VALUE SPACE.      IX1064.2
055200                                                                  IX1064.2
055300 01  CCVS-H-2B.                                                   IX1064.2
055400   02  FILLER                        PIC X(15)  VALUE             IX1064.2
055500            "TEST RESULT OF ".                                    IX1064.2
055600   02  TEST-ID                       PIC X(9).                    IX1064.2
055700   02  FILLER                        PIC X(4)   VALUE             IX1064.2
055800            " IN ".                                               IX1064.2
055900   02  FILLER                        PIC X(12)  VALUE             IX1064.2
056000     " HIGH       ".                                              IX1064.2
056100   02  FILLER                        PIC X(22)  VALUE             IX1064.2
056200            " LEVEL VALIDATION FOR ".                             IX1064.2
056300   02  FILLER                        PIC X(58)  VALUE             IX1064.2
056400     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IX1064.2
056500 01  CCVS-H-3.                                                    IX1064.2
056600     02  FILLER                      PIC X(34)  VALUE             IX1064.2
056700            " FOR OFFICIAL USE ONLY    ".                         IX1064.2
056800     02  FILLER                      PIC X(58)  VALUE             IX1064.2
056900     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".IX1064.2
057000     02  FILLER                      PIC X(28)  VALUE             IX1064.2
057100            "  COPYRIGHT   1985 ".                                IX1064.2
057200 01  CCVS-E-1.                                                    IX1064.2
057300     02 FILLER                       PIC X(52)  VALUE SPACE.      IX1064.2
057400     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              IX1064.2
057500     02 ID-AGAIN                     PIC X(9).                    IX1064.2
057600     02 FILLER                       PIC X(45)  VALUE SPACES.     IX1064.2
057700 01  CCVS-E-2.                                                    IX1064.2
057800     02  FILLER                      PIC X(31)  VALUE SPACE.      IX1064.2
057900     02  FILLER                      PIC X(21)  VALUE SPACE.      IX1064.2
058000     02 CCVS-E-2-2.                                               IX1064.2
058100         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      IX1064.2
058200         03 FILLER                   PIC X      VALUE SPACE.      IX1064.2
058300         03 ENDER-DESC               PIC X(44)  VALUE             IX1064.2
058400            "ERRORS ENCOUNTERED".                                 IX1064.2
058500 01  CCVS-E-3.                                                    IX1064.2
058600     02  FILLER                      PIC X(22)  VALUE             IX1064.2
058700            " FOR OFFICIAL USE ONLY".                             IX1064.2
058800     02  FILLER                      PIC X(12)  VALUE SPACE.      IX1064.2
058900     02  FILLER                      PIC X(58)  VALUE             IX1064.2
059000     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IX1064.2
059100     02  FILLER                      PIC X(13)  VALUE SPACE.      IX1064.2
059200     02 FILLER                       PIC X(15)  VALUE             IX1064.2
059300             " COPYRIGHT 1985".                                   IX1064.2
059400 01  CCVS-E-4.                                                    IX1064.2
059500     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      IX1064.2
059600     02 FILLER                       PIC X(4)   VALUE " OF ".     IX1064.2
059700     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      IX1064.2
059800     02 FILLER                       PIC X(40)  VALUE             IX1064.2
059900      "  TESTS WERE EXECUTED SUCCESSFULLY".                       IX1064.2
060000 01  XXINFO.                                                      IX1064.2
060100     02 FILLER                       PIC X(19)  VALUE             IX1064.2
060200            "*** INFORMATION ***".                                IX1064.2
060300     02 INFO-TEXT.                                                IX1064.2
060400       04 FILLER                     PIC X(8)   VALUE SPACE.      IX1064.2
060500       04 XXCOMPUTED                 PIC X(20).                   IX1064.2
060600       04 FILLER                     PIC X(5)   VALUE SPACE.      IX1064.2
060700       04 XXCORRECT                  PIC X(20).                   IX1064.2
060800     02 INF-ANSI-REFERENCE           PIC X(48).                   IX1064.2
060900 01  HYPHEN-LINE.                                                 IX1064.2
061000     02 FILLER  PIC IS X VALUE IS SPACE.                          IX1064.2
061100     02 FILLER  PIC IS X(65)    VALUE IS "************************IX1064.2
061200-    "*****************************************".                 IX1064.2
061300     02 FILLER  PIC IS X(54)    VALUE IS "************************IX1064.2
061400-    "******************************".                            IX1064.2
061500 01  CCVS-PGM-ID                     PIC X(9)   VALUE             IX1064.2
061600     "IX106A".                                                    IX1064.2
061700 PROCEDURE DIVISION.                                              IX1064.2
061800 CCVS1 SECTION.                                                   IX1064.2
061900 OPEN-FILES.                                                      IX1064.2
062000*    OPEN I-O RAW-DATA.                                           IX1064.2
062100*    MOVE CCVS-PGM-ID TO RAW-DATA-KEY.                            IX1064.2
062200*    READ RAW-DATA INVALID KEY GO TO END-E-1.                     IX1064.2
062300*    MOVE "ABORTED " TO C-ABORT.                                  IX1064.2
062400*    ADD 1 TO C-NO-OF-TESTS.                                      IX1064.2
062500*    ACCEPT C-DATE  FROM DATE.                                    IX1064.2
062600*    ACCEPT C-TIME  FROM TIME.                                    IX1064.2
062700*    REWRITE RAW-DATA-SATZ INVALID KEY GO TO END-E-1.             IX1064.2
062800*END-E-1.                                                         IX1064.2
062900*    CLOSE RAW-DATA.                                              IX1064.2
063000     OPEN    OUTPUT PRINT-FILE.                                   IX1064.2
063100     MOVE  CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.  IX1064.2
063200     MOVE    SPACE TO TEST-RESULTS.                               IX1064.2
063300     PERFORM HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.              IX1064.2
063400     MOVE    ZERO TO REC-SKL-SUB.                                 IX1064.2
063500     PERFORM CCVS-INIT-FILE 9 TIMES.                              IX1064.2
063600 CCVS-INIT-FILE.                                                  IX1064.2
063700     ADD     1 TO REC-SKL-SUB.                                    IX1064.2
063800     MOVE    FILE-RECORD-INFO-SKELETON                            IX1064.2
063900          TO FILE-RECORD-INFO (REC-SKL-SUB).                      IX1064.2
064000 CCVS-INIT-EXIT.                                                  IX1064.2
064100     GO TO CCVS1-EXIT.                                            IX1064.2
064200 CLOSE-FILES.                                                     IX1064.2
064300*    OPEN I-O RAW-DATA.                                           IX1064.2
064400*    MOVE CCVS-PGM-ID TO RAW-DATA-KEY.                            IX1064.2
064500*    READ RAW-DATA INVALID KEY GO TO END-E-2.                     IX1064.2
064600*    MOVE "OK.     " TO C-ABORT.                                  IX1064.2
064700*    MOVE PASS-COUNTER TO C-OK.                                   IX1064.2
064800*    MOVE ERROR-HOLD   TO C-ALL.                                  IX1064.2
064900*    MOVE ERROR-COUNTER TO C-FAIL.                                IX1064.2
065000*    MOVE DELETE-COUNTER TO C-DELETED.                            IX1064.2
065100*    MOVE INSPECT-COUNTER TO C-INSPECT.                           IX1064.2
065200*    REWRITE RAW-DATA-SATZ INVALID KEY GO TO END-E-2.             IX1064.2
065300*END-E-2.                                                         IX1064.2
065400*    CLOSE RAW-DATA.                                              IX1064.2
065500     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   IX1064.2
065600 TERMINATE-CCVS.                                                  IX1064.2
065700*    EXIT PROGRAM.                                                IX1064.2
065800*TERMINATE-CALL.                                                  IX1064.2
065900     STOP     RUN.                                                IX1064.2
066000 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         IX1064.2
066100 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           IX1064.2
066200 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          IX1064.2
066300 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      IX1064.2
066400     MOVE "****TEST DELETED****" TO RE-MARK.                      IX1064.2
066500 PRINT-DETAIL.                                                    IX1064.2
066600     IF REC-CT NOT EQUAL TO ZERO                                  IX1064.2
066700             MOVE "." TO PARDOT-X                                 IX1064.2
066800             MOVE REC-CT TO DOTVALUE.                             IX1064.2
066900     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      IX1064.2
067000     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               IX1064.2
067100        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 IX1064.2
067200          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 IX1064.2
067300     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              IX1064.2
067400     MOVE SPACE TO CORRECT-X.                                     IX1064.2
067500     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         IX1064.2
067600     MOVE     SPACE TO RE-MARK.                                   IX1064.2
067700 HEAD-ROUTINE.                                                    IX1064.2
067800     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  IX1064.2
067900     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  IX1064.2
068000     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  IX1064.2
068100     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  IX1064.2
068200 COLUMN-NAMES-ROUTINE.                                            IX1064.2
068300     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IX1064.2
068400     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IX1064.2
068500     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        IX1064.2
068600 END-ROUTINE.                                                     IX1064.2
068700     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.IX1064.2
068800 END-RTN-EXIT.                                                    IX1064.2
068900     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IX1064.2
069000 END-ROUTINE-1.                                                   IX1064.2
069100      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      IX1064.2
069200      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               IX1064.2
069300      ADD PASS-COUNTER TO ERROR-HOLD.                             IX1064.2
069400*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   IX1064.2
069500      MOVE PASS-COUNTER TO CCVS-E-4-1.                            IX1064.2
069600      MOVE ERROR-HOLD TO CCVS-E-4-2.                              IX1064.2
069700      MOVE CCVS-E-4 TO CCVS-E-2-2.                                IX1064.2
069800      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           IX1064.2
069900  END-ROUTINE-12.                                                 IX1064.2
070000      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        IX1064.2
070100     IF       ERROR-COUNTER IS EQUAL TO ZERO                      IX1064.2
070200         MOVE "NO " TO ERROR-TOTAL                                IX1064.2
070300         ELSE                                                     IX1064.2
070400         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       IX1064.2
070500     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           IX1064.2
070600     PERFORM WRITE-LINE.                                          IX1064.2
070700 END-ROUTINE-13.                                                  IX1064.2
070800     IF DELETE-COUNTER IS EQUAL TO ZERO                           IX1064.2
070900         MOVE "NO " TO ERROR-TOTAL  ELSE                          IX1064.2
071000         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      IX1064.2
071100     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   IX1064.2
071200     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IX1064.2
071300      IF   INSPECT-COUNTER EQUAL TO ZERO                          IX1064.2
071400          MOVE "NO " TO ERROR-TOTAL                               IX1064.2
071500      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   IX1064.2
071600      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            IX1064.2
071700      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          IX1064.2
071800     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IX1064.2
071900 WRITE-LINE.                                                      IX1064.2
072000     ADD 1 TO RECORD-COUNT.                                       IX1064.2
072100*    IF RECORD-COUNT GREATER 42                                   IX1064.2
072200*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          IX1064.2
072300*        MOVE SPACE TO DUMMY-RECORD                               IX1064.2
072400*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  IX1064.2
072500*        MOVE CCVS-H-1  TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES    IX1064.2
072600*        MOVE CCVS-H-2A TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES    IX1064.2
072700*        MOVE CCVS-H-2B TO DUMMY-RECORD PERFORM WRT-LN 3 TIMES    IX1064.2
072800*        MOVE CCVS-H-3  TO DUMMY-RECORD PERFORM WRT-LN 3 TIMES    IX1064.2
072900*        MOVE CCVS-C-1  TO DUMMY-RECORD PERFORM WRT-LN            IX1064.2
073000*        MOVE CCVS-C-2  TO DUMMY-RECORD PERFORM WRT-LN            IX1064.2
073100*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          IX1064.2
073200*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          IX1064.2
073300*        MOVE ZERO TO RECORD-COUNT.                               IX1064.2
073400     PERFORM WRT-LN.                                              IX1064.2
073500 WRT-LN.                                                          IX1064.2
073600     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               IX1064.2
073700     MOVE SPACE TO DUMMY-RECORD.                                  IX1064.2
073800 BLANK-LINE-PRINT.                                                IX1064.2
073900     PERFORM WRT-LN.                                              IX1064.2
074000 FAIL-ROUTINE.                                                    IX1064.2
074100     IF     COMPUTED-X NOT EQUAL TO SPACE                         IX1064.2
074200            GO TO   FAIL-ROUTINE-WRITE.                           IX1064.2
074300     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.IX1064.2
074400     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 IX1064.2
074500     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   IX1064.2
074600     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IX1064.2
074700     MOVE   SPACES TO INF-ANSI-REFERENCE.                         IX1064.2
074800     GO TO  FAIL-ROUTINE-EX.                                      IX1064.2
074900 FAIL-ROUTINE-WRITE.                                              IX1064.2
075000     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         IX1064.2
075100     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 IX1064.2
075200     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. IX1064.2
075300     MOVE   SPACES TO COR-ANSI-REFERENCE.                         IX1064.2
075400 FAIL-ROUTINE-EX. EXIT.                                           IX1064.2
075500 BAIL-OUT.                                                        IX1064.2
075600     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   IX1064.2
075700     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           IX1064.2
075800 BAIL-OUT-WRITE.                                                  IX1064.2
075900     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  IX1064.2
076000     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 IX1064.2
076100     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IX1064.2
076200     MOVE   SPACES TO INF-ANSI-REFERENCE.                         IX1064.2
076300 BAIL-OUT-EX. EXIT.                                               IX1064.2
076400 CCVS1-EXIT.                                                      IX1064.2
076500     EXIT.                                                        IX1064.2
076600******************************************************************IX1064.2
076700 SECT-0001-RIS101 SECTION.                                        IX1064.2
076800*                                                                 IX1064.2
076900* THIS SECTION CREATES A RELATIVE FILE RANDOMLY.                  IX1064.2
077000*                                                                 IX1064.2
077100 WRITE-INT-GF-01.                                                 IX1064.2
077200     OPEN    OUTPUT RL-FR1.                                       IX1064.2
077300     MOVE    "RL-FR1" TO XFILE-NAME           (1).                IX1064.2
077400     MOVE    "R1-F-G" TO  XRECORD-NAME        (1).                IX1064.2
077500     MOVE    ZERO     TO  XRECORD-NUMBER      (1).                IX1064.2
077600     MOVE    "RC"     TO  CHARS-OR-RECORDS    (1).                IX1064.2
077700     MOVE    CCVS-PGM-ID TO  XPROGRAM-NAME    (1).                IX1064.2
077800     MOVE    241      TO  XRECORD-LENGTH      (1).                IX1064.2
077900     MOVE    001      TO  XBLOCK-SIZE         (1).                IX1064.2
078000     MOVE    "RL"     TO  XFILE-ORGANIZATION  (1).                IX1064.2
078100     MOVE    "S"      TO  XLABEL-TYPE         (1).                IX1064.2
078200     MOVE    225      TO  RECORDS-IN-FILE     (1).                IX1064.2
078300     MOVE    "WRITE RL-FR1   " TO FEATURE.                        IX1064.2
078400     MOVE    "WRITE-TEST-GF-01" TO PAR-NAME.                      IX1064.2
078500     MOVE    ZERO TO KEYSUB.                                      IX1064.2
078600     MOVE    ZERO TO INVKEY-COUNTER-RL.                           IX1064.2
078700 WRITE-INIT-GF-01-01.                                             IX1064.2
078800     PERFORM WRITE-TEST-GF-01-R1 75 TIMES.                        IX1064.2
078900     GO TO   WRITE-TEST-GF-01.                                    IX1064.2
079000 WRITE-TEST-GF-01-R1.                                             IX1064.2
079100*                                                                 IX1064.2
079200* WRITE-TEST-GF-01-R1 - WRITES THREE RECORDS FOR EACH PASS THRU . IX1064.2
079300*                      1) FOR THE FIRST RECORD CREATED RL-KEY IS  IX1064.2
079400*                         SET TO FR1-RECKEY-11-13 .               IX1064.2
079500*                      2) FOR THE SECOND RECORD CREATED RL-KEY IS IX1064.2
079600*                         SET TO FR1-ALTKEY1-11-13 .              IX1064.2
079700*                      3) FOR THE THIRD RECORD CREATED RL-KEY IS  IX1064.2
079800*                         SET TO FR1-ALTKEY2-11-13 .              IX1064.2
079900*                      THESE RECORD KEYS ARE IN THREE DIFFERENT   IX1064.2
080000*                      ORDERINGS.                                 IX1064.2
080100*                      1) FR1-RECKEY-11-13 ARE THE NUMBERS FROM   IX1064.2
080200*                         225 TO 151 DECENDING .                  IX1064.2
080300*                      2) FR1-ALTKEY1-11-13 ARE THE NUMBERS FROM  IX1064.2
080400*                         1 TO 75 ASCENDING .                     IX1064.2
080500*                      3) FR1-ALTKEY-11-13 ARE THE NUMBERS FROM   IX1064.2
080600*                         76 TO 151 ASCENDING .                   IX1064.2
080700*                                                                 IX1064.2
080800     ADD     001  TO XRECORD-NUMBER (1).                          IX1064.2
080900     ADD     001  TO  KEYSUB.                                     IX1064.2
081000     MOVE    RECKEY-VALUE (KEYSUB) TO FR1-RECKEY-1-13.            IX1064.2
081100     MOVE    ALTKEY1-VALUE (KEYSUB) TO FR1-ALTKEY1-1-20.          IX1064.2
081200     MOVE    ALTKEY2-VALUE (KEYSUB) TO FR1-ALTKEY2-1-20.          IX1064.2
081300     MOVE    WRK-FR1-RECKEY   TO    XRECORD-KEY (1).              IX1064.2
081400     MOVE    WRK-FR1-ALTKEY1  TO ALTERNATE-KEY1 (1).              IX1064.2
081500     MOVE    WRK-FR1-ALTKEY2  TO ALTERNATE-KEY2 (1).              IX1064.2
081600     MOVE    FILE-RECORD-INFO (1)  TO RL-FR1R1-F-G-241.           IX1064.2
081700     MOVE FR1-RECKEY-11-13 TO RL-KEY.                             IX1064.2
081800     WRITE   RL-FR1R1-F-G-241                                     IX1064.2
081900                        INVALID KEY ADD 001 TO INVKEY-COUNTER-RL. IX1064.2
082000     ADD     001 TO EXCUT-COUNTER-06V00.                          IX1064.2
082100     ADD     001 TO XRECORD-NUMBER (1).                           IX1064.2
082200     MOVE  FR1-ALTKEY1-11-13 TO FR1-RECKEY-11-13.                 IX1064.2
082300     MOVE WRK-FR1-RECKEY TO XRECORD-KEY (1).                      IX1064.2
082400     MOVE FR1-RECKEY-11-13 TO RL-KEY.                             IX1064.2
082500     MOVE FILE-RECORD-INFO (1) TO RL-FR1R1-F-G-241.               IX1064.2
082600     WRITE   RL-FR1R1-F-G-241                                     IX1064.2
082700                        INVALID KEY ADD 001 TO INVKEY-COUNTER-RL. IX1064.2
082800     ADD     001 TO EXCUT-COUNTER-06V00.                          IX1064.2
082900     ADD     001 TO XRECORD-NUMBER (1).                           IX1064.2
083000     MOVE  FR1-ALTKEY2-11-13 TO FR1-RECKEY-11-13.                 IX1064.2
083100     MOVE WRK-FR1-RECKEY TO XRECORD-KEY (1).                      IX1064.2
083200     MOVE FR1-RECKEY-11-13 TO RL-KEY.                             IX1064.2
083300     MOVE FILE-RECORD-INFO (1) TO RL-FR1R1-F-G-241.               IX1064.2
083400     WRITE   RL-FR1R1-F-G-241                                     IX1064.2
083500                        INVALID KEY ADD 001 TO INVKEY-COUNTER-RL. IX1064.2
083600     ADD      001 TO EXCUT-COUNTER-06V00.                         IX1064.2
083700 WRITE-TEST-GF-01.                                                IX1064.2
083800     SUBTRACT INVKEY-COUNTER-RL FROM EXCUT-COUNTER-06V00          IX1064.2
083900             GIVING RECORDS-WRITTEN.                              IX1064.2
084000 WRITE-TEST-CHECK.                                                IX1064.2
084100     MOVE    225  TO CORRECT-18V0.                                IX1064.2
084200     MOVE    RECORDS-WRITTEN TO COMPUTED-18V0.                    IX1064.2
084300     IF      RECORDS-WRITTEN EQUAL TO 225                         IX1064.2
084400             PERFORM  PASS                                        IX1064.2
084500             ELSE                                                 IX1064.2
084600             MOVE "VIII-37  4.9.2                    " TO RE-MARK IX1064.2
084700             PERFORM FAIL.                                        IX1064.2
084800     PERFORM PRINT-DETAIL.                                        IX1064.2
084900 WRITE-TEST-GF-01-END.                                            IX1064.2
085000     CLOSE   RL-FR1.                                              IX1064.2
085100******************************************************************IX1064.2
085200 SECT-0002-RIS101 SECTION.                                        IX1064.2
085300*                                                                 IX1064.2
085400* THIS SECTION CREATES A SEQUENTIAL AND AN INDEXED FILE USING THE IX1064.2
085500* RELATIVE FILE CREATED IN THE PREVIOUS SECTION AS INPUT .        IX1064.2
085600*                                                                 IX1064.2
085700 WRITE-INIT-GF-02.                                                IX1064.2
085800     OPEN    OUTPUT IX-FS1 SQ-FS1.                                IX1064.2
085900     OPEN    INPUT RL-FR1.                                        IX1064.2
086000     MOVE    ZERO TO RL-KEY.                                      IX1064.2
086100     MOVE    ZERO TO INVKEY-COUNTER-RL.                           IX1064.2
086200     MOVE    ZERO TO INVKEY-COUNTER-IX.                           IX1064.2
086300     MOVE ZERO TO EXCUT-COUNTER-06V00.                            IX1064.2
086400     PERFORM WRITE-TEST-GF-02-01 75 TIMES.                        IX1064.2
086500     PERFORM WRITE-TEST-GF-02-02 75 TIMES.                        IX1064.2
086600     PERFORM WRITE-TEST-GF-02-03 75 TIMES.                        IX1064.2
086700     GO TO WRITE-TEST-GF-02-END.                                  IX1064.2
086800 WRITE-INT-GF-02-IX.                                              IX1064.2
086900     MOVE    "WRITE IX-FS1  " TO FEATURE.                         IX1064.2
087000     MOVE    "WRITE-TEST-GF-02" TO PAR-NAME.                      IX1064.2
087100 WRITE-INT-GF-02-SQ.                                              IX1064.2
087200     MOVE    "WRITE SQ-FS1  " TO FEATURE.                         IX1064.2
087300     MOVE    "WRITE-TEST-GF-02" TO PAR-NAME.                      IX1064.2
087400 WRITE-TEST-GF-02-01.                                             IX1064.2
087500*                                                                 IX1064.2
087600* WRITE-TEST-GF-02-01 - READS IN THE RELATIVE FILE IN SEQUENCE ,  IX1064.2
087700*                      MOVES RL-FR1-ALTKEY1-11-13 TO              IX1064.2
087800*                      RL-FR1-ALTKEY2-11-13 SO THAT ALL THE       IX1064.2
087900*                      KEYS ON THE RECORD ARE THE SAME , THEN     IX1064.2
088000*                      WRITES A SEQUENTIAL RECORD AND AN INDEXED  IX1064.2
088100*                      FILE RECORD .                              IX1064.2
088200*                                                                 IX1064.2
088300     ADD     001 TO RL-KEY.                                       IX1064.2
088400     READ RL-FR1                                                  IX1064.2
088500         INVALID KEY ADD 001 TO INVKEY-COUNTER-RL.                IX1064.2
088600     MOVE    RL-FR1-REC-120 TO FILE-RECORD-INFO-P1-120(1).        IX1064.2
088700     MOVE    RL-FR1-ALTKEY1-11-13 TO RL-FR1-ALTKEY2-11-13.        IX1064.2
088800     MOVE    RL-FR1R1-F-G-241 TO SQ-FS1R1-F-G-241.                IX1064.2
088900     MOVE    SQ-FS1R1-F-G-241 TO IX-FS1R1-F-G-241.                IX1064.2
089000     MOVE    "SQ-FS1" TO XFILE-NAME           (1).                IX1064.2
089100     MOVE    "SQ"     TO  XFILE-ORGANIZATION  (1).                IX1064.2
089200     MOVE    "WRITE SQ-FS1  " TO FEATURE.                         IX1064.2
089300     MOVE    "WRITE-TEST-GF-02" TO PAR-NAME.                      IX1064.2
089400     MOVE FILE-RECORD-INFO-P1-120(1) TO SQ-FS1-REC-120.           IX1064.2
089500     WRITE   SQ-FS1R1-F-G-241.                                    IX1064.2
089600     MOVE    "IX-FS1" TO XFILE-NAME           (1).                IX1064.2
089700     MOVE    "IX"     TO  XFILE-ORGANIZATION  (1).                IX1064.2
089800     MOVE    "WRITE IX-FS1  " TO FEATURE.                         IX1064.2
089900     MOVE    "WRITE-TEST-GF-02" TO PAR-NAME.                      IX1064.2
090000     MOVE FILE-RECORD-INFO-P1-120(1) TO IX-FS1-REC-120.           IX1064.2
090100     WRITE   IX-FS1R1-F-G-241                                     IX1064.2
090200         INVALID KEY ADD 001 TO INVKEY-COUNTER-IX.                IX1064.2
090300     ADD 001 TO EXCUT-COUNTER-06V00.                              IX1064.2
090400 WRITE-TEST-GF-02-02.                                             IX1064.2
090500*                                                                 IX1064.2
090600* WRITE-TEST-GF-02-02 - READS IN THE RELATIVE FILE IN SEQUENCE    IX1064.2
090700*                 STARTING AT NUMBER 76 WHERE WRITE-TEST-GF-02-01 IX1064.2
090800*                     LEFT OFF . MOVES RL-FR1-ALTKEY2-11-13 TO    IX1064.2
090900*                     RL-FR1-ALTKEY1-11-13 SO THAT ALL THE KEYS   IX1064.2
091000*                     IN THE RECORD ARE THE SAME . WRITES A       IX1064.2
091100*                     SEQUENTIAL AND AN INDEXED FILE RECORD .     IX1064.2
091200*                                                                 IX1064.2
091300     ADD     001 TO RL-KEY.                                       IX1064.2
091400     READ RL-FR1                                                  IX1064.2
091500         INVALID KEY ADD 001 TO INVKEY-COUNTER-RL.                IX1064.2
091600     MOVE    RL-FR1-REC-120 TO FILE-RECORD-INFO-P1-120(1).        IX1064.2
091700     MOVE    RL-FR1-ALTKEY2-11-13 TO RL-FR1-ALTKEY1-11-13.        IX1064.2
091800     MOVE    RL-FR1R1-F-G-241 TO SQ-FS1R1-F-G-241.                IX1064.2
091900     MOVE    SQ-FS1R1-F-G-241 TO IX-FS1R1-F-G-241.                IX1064.2
092000     MOVE    "SQ-FS1" TO XFILE-NAME           (1).                IX1064.2
092100     MOVE    "SQ"     TO  XFILE-ORGANIZATION  (1).                IX1064.2
092200     MOVE FILE-RECORD-INFO-P1-120(1) TO SQ-FS1-REC-120.           IX1064.2
092300     WRITE   SQ-FS1R1-F-G-241.                                    IX1064.2
092400     MOVE    "IX-FS1" TO XFILE-NAME           (1).                IX1064.2
092500     MOVE    "IX"     TO  XFILE-ORGANIZATION  (1).                IX1064.2
092600     MOVE FILE-RECORD-INFO-P1-120(1) TO IX-FS1-REC-120.           IX1064.2
092700     WRITE   IX-FS1R1-F-G-241                                     IX1064.2
092800         INVALID KEY ADD 001 TO INVKEY-COUNTER-IX.                IX1064.2
092900     ADD 001 TO EXCUT-COUNTER-06V00.                              IX1064.2
093000 WRITE-TEST-GF-02-03.                                             IX1064.2
093100*                                                                 IX1064.2
093200* WRITE-TEST-GF-02-03 - READS IN THE RELATIVE FILE IN SEQUENCE    IX1064.2
093300*                STARTING AT NUMBER 151 WHERE WRITE-TEST-GF-02-02 IX1064.2
093400*                    LEFT OFF . MOVES RL-FR1-KEY-11-13 TO         IX1064.2
093500*                    RL-FR1-ALTKEY1-11-13 AND RL-ALTKEY2-11-13    IX1064.2
093600*                    SO THAT ALL THE KEYS IN THE RECORD ARE       IX1064.2
093700*                    THE SAME . WRITES A SEQUENTIAL AND AN        IX1064.2
093800*                    INDEXED FILE RECORD .                        IX1064.2
093900*                                                                 IX1064.2
094000     ADD     001 TO RL-KEY.                                       IX1064.2
094100     READ RL-FR1                                                  IX1064.2
094200         INVALID KEY ADD 001 TO INVKEY-COUNTER-RL.                IX1064.2
094300     MOVE    RL-FR1-REC-120 TO FILE-RECORD-INFO-P1-120(1).        IX1064.2
094400     MOVE RL-FR1-KEY-11-13 TO RL-FR1-ALTKEY1-11-13.               IX1064.2
094500     MOVE    RL-FR1-ALTKEY1-11-13 TO RL-FR1-ALTKEY2-11-13.        IX1064.2
094600     MOVE    RL-FR1R1-F-G-241 TO SQ-FS1R1-F-G-241.                IX1064.2
094700     MOVE    SQ-FS1R1-F-G-241 TO IX-FS1R1-F-G-241.                IX1064.2
094800     MOVE    "SQ-FS1" TO XFILE-NAME           (1).                IX1064.2
094900     MOVE    "SQ"     TO  XFILE-ORGANIZATION  (1).                IX1064.2
095000     MOVE FILE-RECORD-INFO-P1-120(1) TO SQ-FS1-REC-120.           IX1064.2
095100     WRITE   SQ-FS1R1-F-G-241.                                    IX1064.2
095200     MOVE    "IX-FS1" TO XFILE-NAME           (1).                IX1064.2
095300     MOVE    "IX"     TO  XFILE-ORGANIZATION  (1).                IX1064.2
095400     MOVE FILE-RECORD-INFO-P1-120(1) TO IX-FS1-REC-120.           IX1064.2
095500     WRITE   IX-FS1R1-F-G-241                                     IX1064.2
095600         INVALID KEY ADD 001 TO INVKEY-COUNTER-IX.                IX1064.2
095700     ADD 001 TO EXCUT-COUNTER-06V00.                              IX1064.2
095800 WRITE-TEST-GF-02-END.                                            IX1064.2
095900     CLOSE RL-FR1.                                                IX1064.2
096000     CLOSE SQ-FS1.                                                IX1064.2
096100     CLOSE IX-FS1.                                                IX1064.2
096200     MOVE "READ RL-FR1  " TO FEATURE.                             IX1064.2
096300     MOVE "WRITE-TEST-GF-02" TO PAR-NAME.                         IX1064.2
096400     PERFORM WRITE-TEST-GF-01 THRU WRITE-TEST-CHECK.              IX1064.2
096500     SUBTRACT INVKEY-COUNTER-IX FROM EXCUT-COUNTER-06V00          IX1064.2
096600          GIVING RECORDS-WRITTEN.                                 IX1064.2
096700 WRITE-TEST-GF-02-2.                                              IX1064.2
096800     PERFORM WRITE-INT-GF-02-IX.                                  IX1064.2
096900     MOVE "WRITE-TEST-GF-02-2" TO PAR-NAME.                       IX1064.2
097000     PERFORM WRITE-TEST-CHECK.                                    IX1064.2
097100 WRITE-TEST-GF-02-3.                                              IX1064.2
097200     PERFORM WRITE-INT-GF-02-SQ.                                  IX1064.2
097300     MOVE "WRITE-TEST-GF-02-3" TO PAR-NAME.                       IX1064.2
097400     PERFORM WRITE-TEST-CHECK.                                    IX1064.2
097500******************************************************************IX1064.2
097600 SECT-0003-RIS101 SECTION.                                        IX1064.2
097700*                                                                 IX1064.2
097800* THIS SECTION TESTS THE ABILITY TO HAVE ALL THREE DIFFERNT FILE  IX1064.2
097900* TYPES OPENED AND USED AT THE SAME TIME .                        IX1064.2
098000*                                                                 IX1064.2
098100 READ-TEST-F2-01-01.                                              IX1064.2
098200     MOVE ZEROES TO REC-CT.                                       IX1064.2
098300     OPEN INPUT RL-FR1.                                           IX1064.2
098400     OPEN INPUT IX-FS1.                                           IX1064.2
098500     OPEN INPUT SQ-FS1.                                           IX1064.2
098600     MOVE    "OPEN ALL - SEPARATE" TO FEATURE.                    IX1064.2
098700     MOVE    "READ-TEST-F2-01-02" TO PAR-NAME.                    IX1064.2
098800 READ-TEST-F2-01-02.                                              IX1064.2
098900*                                                                 IX1064.2
099000* READ-TEST-F2-01-02 - THIS TESTS THE ABILITY TO READ ALL THREE   IX1064.2
099100*                      TYPES OF FILES IN ANY ORDER WITH ALL OF THEIX1064.2
099200*                      FILES OPEN AT ONCE .                       IX1064.2
099300*                                                                 IX1064.2
099400     MOVE ZERO TO RL-KEY.                                         IX1064.2
099500     ADD 001 TO RL-KEY.                                           IX1064.2
099600     READ SQ-FS1 INTO IX-FS1R1-F-G-241 AT END GO TEST-FINISH-EXIT.IX1064.2
099700     READ IX-FS1                                                  IX1064.2
099800         INVALID KEY                                              IX1064.2
099900         MOVE "1ST IX-FS1 READ - KEY SHOULD BE 001" TO RE-MARK    IX1064.2
100000         PERFORM FAIL                                             IX1064.2
100100         PERFORM PRINT-DETAIL                                     IX1064.2
100200         GO TO READ-TEST-F2-01-02-END.                            IX1064.2
100300     READ RL-FR1                                                  IX1064.2
100400         INVALID KEY                                              IX1064.2
100500         MOVE "1ST RL-FS1 READ - KEY SHOULD BE 001" TO RE-MARK    IX1064.2
100600         PERFORM FAIL                                             IX1064.2
100700         PERFORM PRINT-DETAIL                                     IX1064.2
100800         GO TO READ-TEST-F2-01-02-END.                            IX1064.2
100900     READ SQ-FS1 AT END GO TO TEST-FINISH-EXIT.                   IX1064.2
101000     READ SQ-FS1 AT END GO TO TEST-FINISH-EXIT.                   IX1064.2
101100     READ RL-FR1                                                  IX1064.2
101200         INVALID KEY                                              IX1064.2
101300         MOVE "2ND IX-FS1 READ - KEY SHOULD BE 001" TO RE-MARK    IX1064.2
101400         PERFORM FAIL                                             IX1064.2
101500         PERFORM PRINT-DETAIL                                     IX1064.2
101600         GO TO READ-TEST-F2-01-02-END.                            IX1064.2
101700     READ IX-FS1                                                  IX1064.2
101800         INVALID KEY                                              IX1064.2
101900         MOVE "2ND RL-FR1 READ - KEY SHOULD BE 001" TO RE-MARK    IX1064.2
102000         PERFORM FAIL                                             IX1064.2
102100         PERFORM PRINT-DETAIL                                     IX1064.2
102200         GO TO READ-TEST-F2-01-02-END.                            IX1064.2
102300     READ SQ-FS1 AT END GO TO TEST-FINISH-EXIT.                   IX1064.2
102400     PERFORM PASS.                                                IX1064.2
102500     PERFORM PRINT-DETAIL.                                        IX1064.2
102600 READ-TEST-F2-01-02-END.                                          IX1064.2
102700     CLOSE RL-FR1.                                                IX1064.2
102800     CLOSE IX-FS1.                                                IX1064.2
102900     CLOSE SQ-FS1.                                                IX1064.2
103000 READ-INIT-F2-02.                                                 IX1064.2
103100     OPEN I-O IX-FS1 SQ-FS1 RL-FR1.                               IX1064.2
103200 READ-TEST-F2-02.                                                 IX1064.2
103300     READ SQ-FS1 AT END GO TO TEST-FINISH-EXIT.                   IX1064.2
103400 READ-WRITE-F2-02.                                                IX1064.2
103500     CLOSE RL-FR1 IX-FS1 SQ-FS1.                                  IX1064.2
103600     MOVE    "R-I-S " TO XFILE-NAME           (1).                IX1064.2
103700     MOVE    ZERO     TO  XRECORD-NUMBER      (1).                IX1064.2
103800     MOVE    "AL"     TO  XFILE-ORGANIZATION  (1).                IX1064.2
103900     MOVE    "OPEN ALL 3 IN 1 LINE" TO FEATURE.                   IX1064.2
104000     MOVE    "READ-TEST-F2-02   " TO PAR-NAME.                    IX1064.2
104100     PERFORM PASS.                                                IX1064.2
104200     PERFORM PRINT-DETAIL.                                        IX1064.2
104300******************************************************************IX1064.2
104400 SECT-0004-RIS101 SECTION.                                        IX1064.2
104500*                                                                 IX1064.2
104600* THIS SECTION TESTS THE ABILITY TO DELETE RECORDS FROM ONE FILE  IX1064.2
104700* TYPE WHILE HAVING THE OTHER FILES OPEN .                        IX1064.2
104800*                                                                 IX1064.2
104900 DELETE-TEST-GF-01.                                               IX1064.2
105000*                                                                 IX1064.2
105100* DELETE-TEST-GF-01-01 -IN THIS TEST RECORD NUMBER 121 IS DELETED IX1064.2
105200*                  THE RELATIVE FILE . THE FILE IS THEN CLOSED .  IX1064.2
105300*                   THE RELATIVE FILE IS THEN RE-OPENED AND       IX1064.2
105400*                   READ EXPECTING TO FIND 122 VALID RECORDS      IX1064.2
105500*                   AND AN INVALID KEY FOR WHAT USED TO BE        IX1064.2
105600*                   THE 122ND RECORD.  RL-FR1-RECKEY-11-13        IX1064.2
105700*                   IS CHECKED TO SEE IF RECORD 123 IS READ       IX1064.2
105800*                   AS IT SHOULD BE.                              IX1064.2
105900*                                                                 IX1064.2
106000     OPEN I-O IX-FS1 RL-FR1 SQ-FS1.                               IX1064.2
106100     MOVE    ZERO TO IX-FS1-KEY-11-13.                            IX1064.2
106200     MOVE    ZERO TO RL-KEY.                                      IX1064.2
106300     MOVE    ZERO TO INVKEY-COUNTER-RL.                           IX1064.2
106400     MOVE    ZERO TO INVKEY-COUNTER-IX.                           IX1064.2
106500     MOVE    121 TO IX-FS1-KEY-11-13.                             IX1064.2
106600     READ    IX-FS1                                               IX1064.2
106700         INVALID KEY ADD 001 TO INVKEY-COUNTER-IX.                IX1064.2
106800     ADD     121 TO RL-KEY.                                       IX1064.2
106900     MOVE    "R-I-S " TO XFILE-NAME           (1)                 IX1064.2
107000     MOVE    ZERO     TO  XRECORD-NUMBER      (1)                 IX1064.2
107100     MOVE    "RL"     TO  XFILE-ORGANIZATION  (1)                 IX1064.2
107200     MOVE    "DELETE RL RECORD" TO FEATURE                        IX1064.2
107300     MOVE    "DELETE-TEST-GF-01   " TO PAR-NAME                   IX1064.2
107400     DELETE RL-FR1                                                IX1064.2
107500         INVALID KEY PERFORM FAIL                                 IX1064.2
107600         PERFORM PRINT-DETAIL                                     IX1064.2
107700         GO TO DELETE-TEST-GF-01-END.                             IX1064.2
107800     CLOSE RL-FR1.                                                IX1064.2
107900     OPEN INPUT RL-FR1.                                           IX1064.2
108000     MOVE ZERO TO RL-KEY.                                         IX1064.2
108100     PERFORM DELETE-TEST-GF-01-02-R2 122 TIMES.                   IX1064.2
108200 DELETE-TEST-GF-01-02-R2.                                         IX1064.2
108300     ADD   1 TO RL-KEY.                                           IX1064.2
108400     READ RL-FR1 INVALID KEY                                      IX1064.2
108500           ADD 1 TO INVKEY-COUNTER-RL.                            IX1064.2
108600 DELETE-TEST-GF-01-02-CK.                                         IX1064.2
108700     IF RL-FR1-KEY-11-13 EQUAL TO 123                             IX1064.2
108800        ADD 1 TO INVKEY-COUNTER-RL.                               IX1064.2
108900        IF                                                        IX1064.2
109000           INVKEY-COUNTER-RL EQUAL TO 2                           IX1064.2
109100           PERFORM PASS ELSE                                      IX1064.2
109200           MOVE "VII-19 4.3.2                        " TO RE-MARK IX1064.2
109300           PERFORM FAIL.                                          IX1064.2
109400     PERFORM PRINT-DETAIL.                                        IX1064.2
109500 DELETE-TEST-GF-01-END.                                           IX1064.2
109600     CLOSE RL-FR1 IX-FS1 SQ-FS1.                                  IX1064.2
109700******************************************************************IX1064.2
109800 SECT-TEST-005-RIS101 SECTION.                                    IX1064.2
109900*                                                                 IX1064.2
110000* THIS SECTION TESTS THE ABILITY TO REWRITE A FILE WHILE OTHER    IX1064.2
110100* FILES ARE BEING MANIPULATED .                                   IX1064.2
110200*                                                                 IX1064.2
110300 REWRITE-INIT-GF-01.                                              IX1064.2
110400     OPEN I-O SQ-FS1 IX-FS1 RL-FR1.                               IX1064.2
110500     MOVE ZEROES TO RL-KEY.                                       IX1064.2
110600     ADD 003 TO RL-KEY.                                           IX1064.2
110700     MOVE    ZERO TO INVKEY-COUNTER-RL.                           IX1064.2
110800     MOVE    ZERO TO INVKEY-COUNTER-IX.                           IX1064.2
110900 REWRITE-TEST-GF-01.                                              IX1064.2
111000*                                                                 IX1064.2
111100* REWRITE-TEST-GF-01:TESTS THE ABILITY TO REWRITE A RELATIVE FILE IX1064.2
111200*                    WHILE ALSO MANIPULATING DATA FROM AN INEXED  IX1064.2
111300*                    FILE .                                       IX1064.2
111400*                                                                 IX1064.2
111500 READ-INIT-RL.                                                    IX1064.2
111600     READ RL-FR1                                                  IX1064.2
111700         INVALID KEY                                              IX1064.2
111800         MOVE    "READ RL RECORD" TO FEATURE                      IX1064.2
111900         MOVE    "READ-INIT-RL      " TO PAR-NAME                 IX1064.2
112000     PERFORM FAIL PERFORM PRINT-DETAIL GO REWRITE-TEST-005-END.   IX1064.2
112100     MOVE RL-FR1-ALTKEY2-11-13 TO IX-FS1-KEY-11-13.               IX1064.2
112200 READ-INIT-IX.                                                    IX1064.2
112300     READ IX-FS1                                                  IX1064.2
112400         INVALID KEY                                              IX1064.2
112500         MOVE    "READ IX RECORD" TO FEATURE                      IX1064.2
112600         MOVE    "READ-INIT-IX.      " TO PAR-NAME                IX1064.2
112700     PERFORM FAIL PERFORM PRINT-DETAIL GO REWRITE-TEST-005-END.   IX1064.2
112800     MOVE RL-FR1-ALTKEY2-11-13 TO IX-FS1-KEY-11-13.               IX1064.2
112900     MOVE IX-FS1R1-F-G-241 TO RL-FR1R1-F-G-241.                   IX1064.2
113000     REWRITE RL-FR1R1-F-G-241                                     IX1064.2
113100         INVALID KEY                                              IX1064.2
113200         MOVE    "REWRITE RL RECORD" TO FEATURE                   IX1064.2
113300         MOVE    "REWRITE-TEST-GF-01"    TO PAR-NAME              IX1064.2
113400     PERFORM FAIL PERFORM PRINT-DETAIL GO REWRITE-TEST-005-END.   IX1064.2
113500 REWRITE-TEST-005-01-1.                                           IX1064.2
113600     READ RL-FR1                                                  IX1064.2
113700         INVALID KEY                                              IX1064.2
113800         MOVE    "READ RL RECORD" TO FEATURE                      IX1064.2
113900         MOVE    "REWRITE-TEST-005-01-1" TO PAR-NAME              IX1064.2
114000       PERFORM FAIL PERFORM PRINT-DETAIL GO REWRITE-TEST-005-END. IX1064.2
114100 REWRITE-TEST-01.                                                 IX1064.2
114200     MOVE    "REWRITE RL RECORD" TO FEATURE                       IX1064.2
114300     MOVE    "REWRITE-TEST-GF-01"    TO PAR-NAME.                 IX1064.2
114400     IF RL-FR1-KEY-11-13 NOT EQUAL 78                             IX1064.2
114500         PERFORM FAIL                                             IX1064.2
114600         MOVE RL-FR1-KEY-11-13 TO COMPUTED-A                      IX1064.2
114700         MOVE "78" TO CORRECT-A                                   IX1064.2
114800         PERFORM PRINT-DETAIL                                     IX1064.2
114900         MOVE     RL-FR1-REC-120 TO DUMMY-RECORD                  IX1064.2
115000         PERFORM  WRITE-LINE                                      IX1064.2
115100         MOVE RL-FR1-REC-121-241 TO DUMMY-RECORD                  IX1064.2
115200         PERFORM WRITE-LINE                                       IX1064.2
115300     ELSE                                                         IX1064.2
115400         PERFORM PASS.                                            IX1064.2
115500     PERFORM PRINT-DETAIL.                                        IX1064.2
115600***************************************************************   IX1064.2
115700 REWRITE-TEST-GF-02.                                              IX1064.2
115800*                                                                 IX1064.2
115900* REWRITE-TEST-GF-02   TESTS THE ABILITY TO REWRITE A SEQUENTIAL  IX1064.2
116000*                      FILE WHILE WORKING WITH A RELATIVE ALSO .  IX1064.2
116100*                                                                 IX1064.2
116200     PERFORM READ-TEST-F2-02       9 TIMES.                       IX1064.2
116300     MOVE 15 TO RL-KEY.                                           IX1064.2
116400     READ RL-FR1 INTO SQ-FS1R1-F-G-241                            IX1064.2
116500         INVALID KEY                                              IX1064.2
116600         MOVE "RL INTO SQ" TO FEATURE                             IX1064.2
116700         MOVE "REWRITE-TEST-GF-02I" TO PAR-NAME                   IX1064.2
116800         MOVE "VIII-26 4.5.2                         " TO RE-MARK IX1064.2
116900         PERFORM FAIL                                             IX1064.2
117000         PERFORM PRINT-DETAIL                                     IX1064.2
117100         GO TEST-FINISH-EXIT.                                     IX1064.2
117200     MOVE "REWRITE-TEST-GF-02 " TO PAR-NAME.                      IX1064.2
117300     MOVE "REWRITE SQ" TO FEATURE.                                IX1064.2
117400     REWRITE SQ-FS1R1-F-G-241.                                    IX1064.2
117500     CLOSE SQ-FS1.                                                IX1064.2
117600     OPEN I-O SQ-FS1.                                             IX1064.2
117700     PERFORM READ-TEST-F2-02       9 TIMES.                       IX1064.2
117800     IF SQ-FS1-ALTKEY2-11-13 EQUAL TO  "090"                      IX1064.2
117900         PERFORM PASS                                             IX1064.2
118000     ELSE                                                         IX1064.2
118100         PERFORM FAIL                                             IX1064.2
118200         MOVE "90" TO CORRECT-A                                   IX1064.2
118300         MOVE SQ-FS1-KEY-11-13 TO COMPUTED-A.                     IX1064.2
118400     PERFORM PRINT-DETAIL.                                        IX1064.2
118500********* END OF 005-02 REWRITE SQ RECORD ************************IX1064.2
118600 REWRITE-TEST-GF-03.                                              IX1064.2
118700*                                                                 IX1064.2
118800* REWRITE-TEST-GF-03  TESTS THE ABILITY TO REWRITE AN INDEXED FILEIX1064.2
118900*                      WHILE ALSO READING A RELATIVE FILE .       IX1064.2
119000*                                                                 IX1064.2
119100     MOVE 15 TO RL-KEY.                                           IX1064.2
119200     MOVE "077" TO IX-FS1-KEY-11-13.                              IX1064.2
119300     MOVE "REWRITE-TEST-GF-03 " TO PAR-NAME.                      IX1064.2
119400     MOVE "REWRITE IX" TO FEATURE.                                IX1064.2
119500     READ IX-FS1                                                  IX1064.2
119600         INVALID KEY                                              IX1064.2
119700         MOVE    "READ IX RECORD" TO FEATURE                      IX1064.2
119800       PERFORM FAIL PERFORM PRINT-DETAIL GO REWRITE-TEST-005-END. IX1064.2
119900     READ RL-FR1                                                  IX1064.2
120000         INVALID KEY                                              IX1064.2
120100         MOVE "READ RL TO PASS TO IX" TO RE-MARK                  IX1064.2
120200         PERFORM FAIL                                             IX1064.2
120300         PERFORM PRINT-DETAIL                                     IX1064.2
120400         GO REWRITE-TEST-005-END.                                 IX1064.2
120500     REWRITE IX-FS1R1-F-G-241 FROM RL-FR1R1-F-G-241               IX1064.2
120600         INVALID KEY                                              IX1064.2
120700         MOVE "REWRITE IX STATEMENT" TO RE-MARK                   IX1064.2
120800         PERFORM FAIL                                             IX1064.2
120900         PERFORM PRINT-DETAIL                                     IX1064.2
121000         GO TO REWRITE-TEST-005-END.                              IX1064.2
121100     MOVE "015" TO IX-FS1-KEY-11-13.                              IX1064.2
121200     READ IX-FS1                                                  IX1064.2
121300         INVALID KEY                                              IX1064.2
121400         MOVE "RE-READ IX FOR CHECK" TO RE-MARK                   IX1064.2
121500         PERFORM FAIL                                             IX1064.2
121600         PERFORM PRINT-DETAIL                                     IX1064.2
121700         GO REWRITE-TEST-005-END.                                 IX1064.2
121800     IF IX-FS1-ALTKEY2-11-13 EQUAL TO "090"                       IX1064.2
121900         PERFORM PASS                                             IX1064.2
122000     ELSE                                                         IX1064.2
122100         MOVE IX-FS1-REC-120 TO DUMMY-RECORD                      IX1064.2
122200         PERFORM WRITE-LINE                                       IX1064.2
122300         MOVE IX-FS1-REC-121-241 TO DUMMY-RECORD                  IX1064.2
122400         PERFORM WRITE-LINE                                       IX1064.2
122500         PERFORM FAIL.                                            IX1064.2
122600     PERFORM PRINT-DETAIL.                                        IX1064.2
122700 REWRITE-TEST-005-END.                                            IX1064.2
122800     CLOSE SQ-FS1 IX-FS1 RL-FR1.                                  IX1064.2
122900 TEST-FINISH-EXIT.                                                IX1064.2
123000     EXIT.                                                        IX1064.2
123100 CCVS-EXIT SECTION.                                               IX1064.2
123200 CCVS-999999.                                                     IX1064.2
123300     GO TO CLOSE-FILES.                                           IX1064.2
      *END-OF,IX106A                                                                  
