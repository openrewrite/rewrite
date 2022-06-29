      *HEADER,COBOL,IX211A                                                            
000100 IDENTIFICATION DIVISION.                                         IX2114.2
000200 PROGRAM-ID.                                                      IX2114.2
000300     IX211A.                                                      IX2114.2
000400****************************************************************  IX2114.2
000500*                                                              *  IX2114.2
000600*    VALIDATION FOR:-                                          *  IX2114.2
000700*                                                              *  IX2114.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IX2114.2
000900*                                                              *  IX2114.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".IX2114.2
001100*                                                              *  IX2114.2
001200****************************************************************  IX2114.2
001300*                                                              *  IX2114.2
001400*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  IX2114.2
001500*                                                              *  IX2114.2
001600*        X-55  - SYSTEM PRINTER NAME.                          *  IX2114.2
001700*        X-82  - SOURCE COMPUTER NAME.                         *  IX2114.2
001800*        X-83  - OBJECT COMPUTER NAME.                         *  IX2114.2
001900*                                                              *  IX2114.2
002000****************************************************************  IX2114.2
002100*         "IX211A"                                                IX2114.2
002200******************************************************************IX2114.2
002300*                                                                 IX2114.2
002400*    THIS PROGRAM TESTS THE CAPABILITY TO CHANGE (UPDATE) INDEX   IX2114.2
002500*    KEYS OF RECORDS IN AN INDEXED I-O FILE AND THEN RETRIEVE THE IX2114.2
002600*    RECORDS FROM THE FILE IN THE PROPER SEQUENCE.  A RECORD      IX2114.2
002700*    MODIFICATION FOR THE FILE WILL INVOLVE THE UPDATING          IX2114.2
002800*    OF THE UPDATE-NUMBER FIELD, THE RECORD-KEY OR ALTERNATE-KEY, IX2114.2
002900*    AND THE ODO-NUMBER FIELD OF THE RECORD.  EACH TIME A GIVEN   IX2114.2
003000*    RECORD IS MODIFIED THE UPDATE-NUMBER FIELD WILL BE INCREMENT-IX2114.2
003100*    ED BY ONE.  TO KEEP TRACK OF THOSE RECORDS MODIFIED,         IX2114.2
003200*    THE ODO-NUMBER FIELD WILL ALWAYS CARRY THE SEQUENTIAL LOC-   IX2114.2
003300*    ATION OF THE RECORD WITHIN THE FILE WHICH REFLECTS THE LAST  IX2114.2
003400*    KEY VALUE POSITION BEFORE THE RECORD WAS MODIFIED.  THIS     IX2114.2
003500*    LOCATION NUMBER WILL BE USED FOR VERIFYING THAT THE SEQUENTI-IX2114.2
003600*    AL REORDING OF THE RECORD FOR THE FILE WAS PROPER.  ONLY ONE IX2114.2
003700*    OF THE 3 KEYS OF THE RECORD WILLBE MODIFIED FOR ANY GIVEN    IX2114.2
003800*    REWIRTE OF THE RECORD.                                       IX2114.2
003900*    FURTHER A TEST IS MADE TO SEE IF THE POSITION INDICATOR      IX2114.2
004000*    IS AFFECTED BY EXECUTION OF THE REWRITE STATEMENT.  IT SHOULDIX2114.2
004100*    NOT (PARAGRAPH 4.6.4 (7), PAGE IX-34).                       IX2114.2
004200*                                                                 IX2114.2
004300*            REFERENCE   AMERICAN NATIONAL STANDARD               IX2114.2
004400*                        PROGRAMMING LANGUAGE COBOL, X3.23-1985.  IX2114.2
004500*                        SECTION IX, INDEXED I-O, THE REWRITE     IX2114.2
004600*                        STATEMENT, PARAGRAPHS 4.6.4 (7),(14), ANDIX2114.2
004700*                                             (15 B)              IX2114.2
004800*                                                                 IX2114.2
004900*                                                                 IX2114.2
005000*    THIS PROGRAM FIRST CREATES AN  INDEXED SEQUENTIAL FILE       IX2114.2
005100*    CONTAINING TWO ALTERNATE KEYS AND  THE ONE REQUIRED RECORD   IX2114.2
005200*    KEY FOR THE FILE.  IMMEDIATELY FOLLOWING FILE CREATION THE   IX2114.2
005300*    FILE IS READ AND THE RECORDS OF THE FILE VERIFIED FOR        IX2114.2
005400*    ACCURACY.                                                    IX2114.2
005500*    THE RECORDS IN THE FILE ARE CREATED IN SEQUENTIAL ORDER BY   IX2114.2
005600*    RECORD KEY VALUE.  FOLLOWING  IS A SAMPLE OF THE DATA        IX2114.2
005700*    CONTENTS FOR THE RECORD KEY AND TWO ALTERNATE RECORD KEYS IN IX2114.2
005800*    THE FILE.                                                    IX2114.2
005900*                                                                 IX2114.2
006000*   REC-NO  RECORD-KEY      ALTERNATE-KEY-1     ALTERNATE-KEY-2   IX2114.2
006100*   ------  ----------      ---------------     ---------------   IX2114.2
006200*    001   BBBBBBBBBC002 EEEEEEEEEF000ALTKEY1 WWWWWWWWWV398ALTKEY2IX2114.2
006300*    002   BBBBBBBBCC004 EEEEEEEEFF004ALTKEY1 WWWWWWWWVV396ALTKEY2IX2114.2
006400*    003   BBBBBBBCCC006 EEEEEEEFFF006ALTKEY1 WWWWWWWVVV394ALTKEY2IX2114.2
006500*     .          .                 .                    .         IX2114.2
006600*     .          .                 .                    .         IX2114.2
006700*     .          .                 .                    .         IX2114.2
006800*    010   CCCCCCCCCC020 FFFFFFFFFF020ALTKEY1 VVVVVVVVVV380ALTKEY2IX2114.2
006900*    011   CCCCCCCCCD022 FFFFFFFFFG022ALTKEY1 VVVVVVVVVV380ALTKEY2IX2114.2
007000*    012   CCCCCCCCDD024 FFFFFFFFGG024ALTKEY1 VVVVVVVVUU376ALTKEY2IX2114.2
007100*     .          .                 .                    .         IX2114.2
007200*     .          .                 .                    .         IX2114.2
007300*     .          .                 .                    .         IX2114.2
007400*    200   UUUUUUUUUU400 YYYYYYYYYY400ALTKEY1 DDDDDDDDDD000ALTKEY2IX2114.2
007500*                                                                 IX2114.2
007600*     NOTE 1 - ALTERNATE KEY NUMBER 2 CONTAINS DUPLICATE KEYS     IX2114.2
007700*              EVERY 10TH AND 11TH RECORDS.                       IX2114.2
007800*                                                                 IX2114.2
007900*    NOTE 2 - THE FIRST 50 RECORDS AND LAST 25 RECORDS OF THE     IX2114.2
008000*    FILE FOLLOW THE ABOVE SEQUENTIAL KEY PATTERN.  FOR THE MIDDLEIX2114.2
008100*    125 RECORDS ONLY THE NUMBER PART OF THE KEYS ARE VARIED      IX2114.2
008200*    AND VARIED IN THE SEQUENCE SHOWN ABOVE.  THAT IS, RECORD-KEY IX2114.2
008300*    AND ALTERNATE-KEY-1 ARE INCREMENTED BY 2 AND THE ALTERNATE-  IX2114.2
008400*    KEY-2 IS DECREMENTED BY 2 EACH TIME A RECORD IS WRITTEN TO   IX2114.2
008500*    THE FILE.  THE FILE IS DESIGNED TO BE LARGE ENOUGH SO THAT   IX2114.2
008600*    AN I-O OPERATION IS REQUIRED FOR EACH RECORD ACCESSED FROM   IX2114.2
008700*    THE FILE.                                                    IX2114.2
008800*                                                                 IX2114.2
008900*    FILE CHARACTERISTICS ARE: FILE SIZE            = 200 RECORDS IX2114.2
009000*                              RECORD SIZE          = 240 CHARS.  IX2114.2
009100*                              RECORD KEY SIZE      = 13  CHARS.  IX2114.2
009200*                              ALTERNATE KEY 1 SIZE = 20  CHARS.  IX2114.2
009300*                              ALTERNATE KEY 2 SIZE = 20  CHARS.  IX2114.2
009400*                              ACCESS MODE          = DYNAMIC     IX2114.2
009500*                                                                 IX2114.2
009600*    A LIST OF COBOL ELEMENTS WITH THE PARAGRAPH NAME IN PARENTH- IX2114.2
009700*    ESIS THAT TESTS THE ELEMENT AND A SHORT DESCRIPTION OF THE   IX2114.2
009800*    TEST FOLLOWS.                                                IX2114.2
009900*                                                                 IX2114.2
010000*      WRITE --- INVALID KEY --. (INX-TEST-001) - THIS TEST       IX2114.2
010100*            CREATES A FILE OF 200 RECORDS CONTAINING A RECORD KEYIX2114.2
010200*            AND 2 ALTERNATE KEYS.                                IX2114.2
010300*      READ --- AT END ---. (INX-TEST-002) - THIS TEST READS THE  IX2114.2
010400*            FILE CREATED IN INX-TEST-001 AND VERIFIES THAT THE   IX2114.2
010500*            FILE WAS CREATED CORRECTLY.                          IX2114.2
010600*      START --- KEY IS EQUAL TO ALTERNAT-KEY1 ---.  AND          IX2114.2
010700*      READ --- NEXT AT END ---. (INX-TEST-003.01) - THIS TEST    IX2114.2
010800*            READS A RECORD AND ESTABLISHES THE ALTERNAT-KEY1 AS  IX2114.2
010900*            THE KEY OF REFERENCE FOR THE FOLLOWING REWRITE TEST. IX2114.2
011000*      REWRITE --- INVALID KEY ---. (INX-TEST-003-02) - THIS TEST IX2114.2
011100*            MODIFIES THE ALTERNATE-KEY1 KEY OF THE RECORD AND    IX2114.2
011200*            REWRITES THE RECORD                                  IX2114.2
011300*      READ --- NEXT AT END ---. (INX-TEST-003.03) - ONE RECORD   IX2114.2
011400*            IS READ SEQUENTIALLY FROM THE FILE.  THE REWRITE     IX2114.2
011500*            IN PREVIOUS TEST SHOULD NOT HAVE AFFECTED THE RECORD IX2114.2
011600*            POINTER FOR THE FILE, THUS THE RECORD MADE AVAILABLE IX2114.2
011700*            SHOULD BE THE NEXT RECORD AS THOUGH THE ALTERNATE KEYIX2114.2
011800*            HAD NOT BEEN MODIFIED.                               IX2114.2
011900*      READ --- NEXT AT END ---. (INX-TEST-003.04) - THIS TEST    IX2114.2
012000*            READS THE NEXT 4 RECORDS SEQUENTIALLY TO SEE IF      IX2114.2
012100*            THE REWRITE OF THE RECORD CAUSED SEQUENTIAL          IX2114.2
012200*            REORDING OF THE RECORDS.                             IX2114.2
012300*                                                                 IX2114.2
012400******************************************************************IX2114.2
012500 ENVIRONMENT DIVISION.                                            IX2114.2
012600 CONFIGURATION SECTION.                                           IX2114.2
012700 SOURCE-COMPUTER.                                                 IX2114.2
012800     XXXXX082.                                                    IX2114.2
012900 OBJECT-COMPUTER.                                                 IX2114.2
013000     XXXXX083.                                                    IX2114.2
013100 INPUT-OUTPUT SECTION.                                            IX2114.2
013200 FILE-CONTROL.                                                    IX2114.2
013300*    SELECT RAW-DATA   ASSIGN TO                                  IX2114.2
013400*    XXXXX062                                                     IX2114.2
013500*           ORGANIZATION IS INDEXED                               IX2114.2
013600*           ACCESS MODE IS RANDOM                                 IX2114.2
013700*           RECORD KEY IS RAW-DATA-KEY.                           IX2114.2
013800     SELECT PRINT-FILE ASSIGN TO                                  IX2114.2
013900     XXXXX055.                                                    IX2114.2
014000     SELECT  IX-FD1                                               IX2114.2
014100     ASSIGN TO                                                    IX2114.2
014200     XXXXX024                                                     IX2114.2
014300*    XXXXX044                                                     IX2114.2
014400        ACCESS MODE IS DYNAMIC                                    IX2114.2
014500        ORGANIZATION IS INDEXED                                   IX2114.2
014600        RECORD KEY IS IX-FD1-KEY                                  IX2114.2
014700        ALTERNATE RECORD KEY IS IX-FD1-ALTKEY1                    IX2114.2
014800        ALTERNATE RECORD KEY IS IX-FD1-ALTKEY2 WITH DUPLICATES    IX2114.2
014900        FILE STATUS IS FD1-STATUS.                                IX2114.2
015000 DATA DIVISION.                                                   IX2114.2
015100 FILE SECTION.                                                    IX2114.2
015200*                                                                 IX2114.2
015300*FD  RAW-DATA.                                                    IX2114.2
015400*                                                                 IX2114.2
015500*01  RAW-DATA-SATZ.                                               IX2114.2
015600*    05  RAW-DATA-KEY        PIC X(6).                            IX2114.2
015700*    05  C-DATE              PIC 9(6).                            IX2114.2
015800*    05  C-TIME              PIC 9(8).                            IX2114.2
015900*    05  C-NO-OF-TESTS       PIC 99.                              IX2114.2
016000*    05  C-OK                PIC 999.                             IX2114.2
016100*    05  C-ALL               PIC 999.                             IX2114.2
016200*    05  C-FAIL              PIC 999.                             IX2114.2
016300*    05  C-DELETED           PIC 999.                             IX2114.2
016400*    05  C-INSPECT           PIC 999.                             IX2114.2
016500*    05  C-NOTE              PIC X(13).                           IX2114.2
016600*    05  C-INDENT            PIC X.                               IX2114.2
016700*    05  C-ABORT             PIC X(8).                            IX2114.2
016800 FD  PRINT-FILE.                                                  IX2114.2
016900 01  PRINT-REC PICTURE X(120).                                    IX2114.2
017000 01  DUMMY-RECORD PICTURE X(120).                                 IX2114.2
017100 FD  IX-FD1                                                       IX2114.2
017200C    LABEL RECORDS ARE STANDARD                                   IX2114.2
017300C    DATA RECORD IS IX-FD1R1-F-G-240                              IX2114.2
017400     RECORD CONTAINS 240 CHARACTERS.                              IX2114.2
017500 01  IX-FD1R1-F-G-240.                                            IX2114.2
017600     05 IX-FD1-REC-120           PICTURE X(120).                  IX2114.2
017700     05 IX-FD1-REC-121-240.                                       IX2114.2
017800        10 FILLER                PICTURE X(8).                    IX2114.2
017900        10 IX-REC-KEY-AREA.                                       IX2114.2
018000           15 IX-FD1-KEY.                                         IX2114.2
018100              20 IX-FD1-KEY-1-10.                                 IX2114.2
018200                 25 IX-FD1-KEY-1-5      PICTURE X(5).             IX2114.2
018300                 25 IX-FD1-KEY-6-10     PICTURE X(5).             IX2114.2
018400               20 IX-FD1-KEY-11-13      PICTURE X(3).             IX2114.2
018500           15 FILLER             PICTURE X(16).                   IX2114.2
018600        10 FILLER                PICTURE X(9).                    IX2114.2
018700        10 IX-ALT-KEY1-AREA.                                      IX2114.2
018800           15 IX-FD1-ALTKEY1.                                     IX2114.2
018900              20 IX-FD1-ALTKEY1-1-10.                             IX2114.2
019000                 25 IX-FD1-ALTKEY1-1-5  PICTURE X(5).             IX2114.2
019100                 25 IX-FD1-ALTKEY1-6-10 PICTURE X(5).             IX2114.2
019200              20 IX-FD1-ALTKEY1-11-13   PICTURE X(3).             IX2114.2
019300              20 IX-FD1-ALTKEY1-14-20   PICTURE X(7).             IX2114.2
019400           15 FILLER             PICTURE X(9).                    IX2114.2
019500        10 FILLER                PICTURE X(9).                    IX2114.2
019600        10 IX-ALT-KEY2-AREA.                                      IX2114.2
019700           15 IX-FD1-ALTKEY2.                                     IX2114.2
019800              20 IX-FD1-ALTKEY2-1-10.                             IX2114.2
019900                 25 IX-FD1-ALTKEY2-1-5  PICTURE X(5).             IX2114.2
020000                 25 IX-FD1-ALTKEY2-6-10 PICTURE X(5).             IX2114.2
020100              20 IX-FD1-ALTKEY2-11-13   PICTURE X(3).             IX2114.2
020200              20 IX-FD1-ALTKEY2-14-20   PICTURE X(7).             IX2114.2
020300           15 FILLER             PICTURE X(9).                    IX2114.2
020400        10 FILLER                PICTURE X(7).                    IX2114.2
020500 WORKING-STORAGE SECTION.                                         IX2114.2
020600 01  WRK-FD1-RECKEY.                                              IX2114.2
020700     05 FD1-RECKEY-1-13.                                          IX2114.2
020800        10 FD1-RECKEY-1-10       PICTURE X(10).                   IX2114.2
020900        10 FD1-RECKEY-11-13      PICTURE 9(3).                    IX2114.2
021000     05 FILLER                   PICTURE X(16)  VALUE SPACE.      IX2114.2
021100 01  WRK-FD1-ALTKEY1.                                             IX2114.2
021200      05 FD1-ALTKEY1-1-20.                                        IX2114.2
021300       10  FD1-ALTKEY1-1-10.                                      IX2114.2
021400        15 FD1-ALTKEY1-1-5       PICTURE X(5).                    IX2114.2
021500        15 FD1-ALTKEY1-6-10      PICTURE X(5).                    IX2114.2
021600       10 FD1-ALTKEY1-11-13      PICTURE 9(3).                    IX2114.2
021700       10 FD1-ALTKEY1-14-20      PICTURE X(7).                    IX2114.2
021800     05 FILLER                   PICTURE X(9)  VALUE SPACE.       IX2114.2
021900 01  WRK-FD1-ALTKEY2.                                             IX2114.2
022000     05 FD1-ALTKEY2-1-20.                                         IX2114.2
022100        10 FD1-ALTKEY2-1-10.                                      IX2114.2
022200           15 FD1-ALTKEY2-1-5    PICTURE X(5).                    IX2114.2
022300           15 FD1-ALTKEY2-6-10   PICTURE X(5).                    IX2114.2
022400        10 FD1-ALTKEY2-11-13     PICTURE 9(3).                    IX2114.2
022500        10 FD1-ALTKEY2-14-20     PICTURE X(7).                    IX2114.2
022600     05 FILLER                   PICTURE X(9) VALUE SPACE.        IX2114.2
022700 01  RECNO                       PICTURE 9(5)  VALUE ZERO.        IX2114.2
022800 01  ADJUSTED-NUM        PIC X(8) VALUE "NO".                     IX2114.2
022900 01  FD1-STATUS                  PICTURE XX   VALUE SPACE.        IX2114.2
023000 01  EXCUT-COUNTER-06V00         PICTURE 9(6) VALUE ZERO.         IX2114.2
023100 01  KEYSUB                      PICTURE 9(3) COMPUTATIONAL.      IX2114.2
023200 01  INVKEY-COUNTER              PICTURE 9(3) COMPUTATIONAL.      IX2114.2
023300 01  RECORDS-WRITTEN             PICTURE 9(3).                    IX2114.2
023400 01  RECKEY-NUM                  PICTURE 9(3).                    IX2114.2
023500 01  ALTKEY1-NUM                 PICTURE 9(3).                    IX2114.2
023600 01  ALTKEY2-NUM                 PICTURE 9(3).                    IX2114.2
023700 01  RECORD-KEY-CONTENT.                                          IX2114.2
023800     05 FILLER PIC X(53) VALUE                                    IX2114.2
023900          "BBBBBBBBBC002EEEEEEEEEF002ALTKEY1WWWWWWWWWV398ALTKEY2".IX2114.2
024000     05 FILLER PIC X(53) VALUE                                    IX2114.2
024100          "BBBBBBBBCC004EEEEEEEEFF004ALTKEY1WWWWWWWWVV396ALTKEY2".IX2114.2
024200     05 FILLER PIC X(53) VALUE                                    IX2114.2
024300          "BBBBBBBCCC006EEEEEEEFFF006ALTKEY1WWWWWWWVVV394ALTKEY2".IX2114.2
024400     05 FILLER PIC X(53) VALUE                                    IX2114.2
024500          "BBBBBBCCCC008EEEEEEFFFF008ALTKEY1WWWWWWVVVV392ALTKEY2".IX2114.2
024600     05 FILLER PIC X(53) VALUE                                    IX2114.2
024700          "BBBBBCCCCC010EEEEEFFFFF010ALTKEY1WWWWWVVVVV390ALTKEY2".IX2114.2
024800     05 FILLER PIC X(53) VALUE                                    IX2114.2
024900          "BBBBCCCCCC012EEEEFFFFFF012ALTKEY1WWWWVVVVVV388ALTKEY2".IX2114.2
025000     05 FILLER PIC X(53) VALUE                                    IX2114.2
025100          "BBBCCCCCCC014EEEFFFFFFF014ALTKEY1WWWVVVVVVV386ALTKEY2".IX2114.2
025200     05 FILLER PIC X(53) VALUE                                    IX2114.2
025300          "BBCCCCCCCC016EEFFFFFFFF016ALTKEY1WWVVVVVVVV384ALTKEY2".IX2114.2
025400     05 FILLER PIC X(53) VALUE                                    IX2114.2
025500          "BCCCCCCCCC018EFFFFFFFFF018ALTKEY1WVVVVVVVVV382ALTKEY2".IX2114.2
025600     05 FILLER PIC X(53) VALUE                                    IX2114.2
025700          "CCCCCCCCCC020FFFFFFFFFF020ALTKEY1VVVVVVVVVV380ALTKEY2".IX2114.2
025800     05 FILLER PIC X(53) VALUE                                    IX2114.2
025900          "CCCCCCCCCD022FFFFFFFFFG022ALTKEY1VVVVVVVVVV380ALTKEY2".IX2114.2
026000     05 FILLER PIC X(53) VALUE                                    IX2114.2
026100          "CCCCCCCCDD024FFFFFFFFGG024ALTKEY1VVVVVVVVUU376ALTKEY2".IX2114.2
026200     05 FILLER PIC X(53) VALUE                                    IX2114.2
026300          "CCCCCCCDDD026FFFFFFFGGG026ALTKEY1VVVVVVVUUU374ALTKEY2".IX2114.2
026400     05 FILLER PIC X(53) VALUE                                    IX2114.2
026500          "CCCCCCDDDD028FFFFFFGGGG028ALTKEY1VVVVVVUUUU372ALTKEY2".IX2114.2
026600     05 FILLER PIC X(53) VALUE                                    IX2114.2
026700          "CCCCCDDDDD030FFFFFGGGGG030ALTKEY1VVVVVUUUUU370ALTKEY2".IX2114.2
026800     05 FILLER PIC X(53) VALUE                                    IX2114.2
026900          "CCCCDDDDDD032FFFFGGGGGG032ALTKEY1VVVVUUUUUU368ALTKEY2".IX2114.2
027000     05 FILLER PIC X(53) VALUE                                    IX2114.2
027100          "CCCDDDDDDD034FFFGGGGGGG034ALTKEY1VVVUUUUUUU366ALTKEY2".IX2114.2
027200     05 FILLER PIC X(53) VALUE                                    IX2114.2
027300          "CCDDDDDDDD036FFGGGGGGGG036ALTKEY1VVUUUUUUUU364ALTKEY2".IX2114.2
027400     05 FILLER PIC X(53) VALUE                                    IX2114.2
027500          "CDDDDDDDDD038FGGGGGGGGG038ALTKEY1VUUUUUUUUU362ALTKEY2".IX2114.2
027600     05 FILLER PIC X(53) VALUE                                    IX2114.2
027700          "DDDDDDDDDD040GGGGGGGGGG040ALTKEY1UUUUUUUUUU360ALTKEY2".IX2114.2
027800     05 FILLER PIC X(53) VALUE                                    IX2114.2
027900          "DDDDDDDDDE042GGGGGGGGGH042ALTKEY1UUUUUUUUUU360ALTKEY2".IX2114.2
028000     05 FILLER PIC X(53) VALUE                                    IX2114.2
028100          "DDDDDDDDEE044GGGGGGGGHH044ALTKEY1UUUUUUUUTT356ALTKEY2".IX2114.2
028200     05 FILLER PIC X(53) VALUE                                    IX2114.2
028300          "DDDDDDDEEE046GGGGGGGHHH046ALTKEY1UUUUUUUTTT354ALTKEY2".IX2114.2
028400     05 FILLER PIC X(53) VALUE                                    IX2114.2
028500          "DDDDDDEEEE048GGGGGGHHHH048ALTKEY1UUUUUUTTTT352ALTKEY2".IX2114.2
028600     05 FILLER PIC X(53) VALUE                                    IX2114.2
028700          "DDDDDEEEEE050GGGGGHHHHH050ALTKEY1UUUUUTTTTT350ALTKEY2".IX2114.2
028800     05 FILLER PIC X(53) VALUE                                    IX2114.2
028900          "DDDDEEEEEE052GGGGHHHHHH052ALTKEY1UUUUTTTTTT348ALTKEY2".IX2114.2
029000     05 FILLER PIC X(53) VALUE                                    IX2114.2
029100          "DDDEEEEEEE054GGGHHHHHHH054ALTKEY1UUUTTTTTTT346ALTKEY2".IX2114.2
029200     05 FILLER PIC X(53) VALUE                                    IX2114.2
029300          "DDEEEEEEEE056GGHHHHHHHH056ALTKEY1UUTTTTTTTT344ALTKEY2".IX2114.2
029400     05 FILLER PIC X(53) VALUE                                    IX2114.2
029500          "DEEEEEEEEE058GHHHHHHHHH058ALTKEY1UTTTTTTTTT342ALTKEY2".IX2114.2
029600     05 FILLER PIC X(53) VALUE                                    IX2114.2
029700          "EEEEEEEEEE060HHHHHHHHHH060ALTKEY1TTTTTTTTTT340ALTKEY2".IX2114.2
029800     05 FILLER PIC X(53) VALUE                                    IX2114.2
029900          "EEEEEEEEEF062HHHHHHHHHI062ALTKEY1TTTTTTTTTT340ALTKEY2".IX2114.2
030000     05 FILLER PIC X(53) VALUE                                    IX2114.2
030100          "EEEEEEEEFF064HHHHHHHHII064ALTKEY1TTTTTTTTSS336ALTKEY2".IX2114.2
030200     05 FILLER PIC X(53) VALUE                                    IX2114.2
030300          "EEEEEEEFFF066HHHHHHHIII066ALTKEY1TTTTTTTSSS334ALTKEY2".IX2114.2
030400     05 FILLER PIC X(53) VALUE                                    IX2114.2
030500          "EEEEEEFFFF068HHHHHHIIII068ALTKEY1TTTTTTSSSS332ALTKEY2".IX2114.2
030600     05 FILLER PIC X(53) VALUE                                    IX2114.2
030700          "EEEEEFFFFF070HHHHHIIIII070ALTKEY1TTTTTSSSSS330ALTKEY2".IX2114.2
030800     05 FILLER PIC X(53) VALUE                                    IX2114.2
030900          "EEEEFFFFFF072HHHHIIIIII072ALTKEY1TTTTSSSSSS328ALTKEY2".IX2114.2
031000     05 FILLER PIC X(53) VALUE                                    IX2114.2
031100          "EEEFFFFFFF074HHHIIIIIII074ALTKEY1TTTSSSSSSS326ALTKEY2".IX2114.2
031200     05 FILLER PIC X(53) VALUE                                    IX2114.2
031300          "EEFFFFFFFF076HHIIIIIIII076ALTKEY1TTSSSSSSSS324ALTKEY2".IX2114.2
031400     05 FILLER PIC X(53) VALUE                                    IX2114.2
031500          "EFFFFFFFFF078HIIIIIIIII078ALTKEY1TSSSSSSSSS322ALTKEY2".IX2114.2
031600     05 FILLER PIC X(53) VALUE                                    IX2114.2
031700          "FFFFFFFFFF080IIIIIIIIII080ALTKEY1SSSSSSSSSS320ALTKEY2".IX2114.2
031800     05 FILLER PIC X(53) VALUE                                    IX2114.2
031900          "FFFFFFFFFG082IIIIIIIIIJ082ALTKEY1SSSSSSSSSS320ALTKEY2".IX2114.2
032000     05 FILLER PIC X(53) VALUE                                    IX2114.2
032100          "FFFFFFFFGG084IIIIIIIIJJ084ALTKEY1SSSSSSSSRR316ALTKEY2".IX2114.2
032200     05 FILLER PIC X(53) VALUE                                    IX2114.2
032300          "FFFFFFFGGG086IIIIIIIJJJ086ALTKEY1SSSSSSSRRR314ALTKEY2".IX2114.2
032400     05 FILLER PIC X(53) VALUE                                    IX2114.2
032500          "FFFFFFGGGG088IIIIIIJJJJ088ALTKEY1SSSSSSRRRR312ALTKEY2".IX2114.2
032600     05 FILLER PIC X(53) VALUE                                    IX2114.2
032700          "FFFFFGGGGG090IIIIIJJJJJ090ALTKEY1SSSSSRRRRR310ALTKEY2".IX2114.2
032800     05 FILLER PIC X(53) VALUE                                    IX2114.2
032900          "FFFFGGGGGG092IIIIJJJJJJ092ALTKEY1SSSSRRRRRR308ALTKEY2".IX2114.2
033000     05 FILLER PIC X(53) VALUE                                    IX2114.2
033100          "FFFGGGGGGG094IIIJJJJJJJ094ALTKEY1SSSRRRRRRR306ALTKEY2".IX2114.2
033200     05 FILLER PIC X(53) VALUE                                    IX2114.2
033300          "FFGGGGGGGG096IIJJJJJJJJ096ALTKEY1SSRRRRRRRR304ALTKEY2".IX2114.2
033400     05 FILLER PIC X(53) VALUE                                    IX2114.2
033500          "FGGGGGGGGG098IJJJJJJJJJ098ALTKEY1SRRRRRRRRR302ALTKEY2".IX2114.2
033600     05 FILLER PIC X(53) VALUE                                    IX2114.2
033700          "GGGGGGGGGG100JJJJJJJJJJ100ALTKEY1RRRRRRRRRR300ALTKEY2".IX2114.2
033800     05 FILLER PIC X(53) VALUE                                    IX2114.2
033900          "RRRRSSSSSS352VVVVWWWWWW352ALTKEY1GGGGFFFFFF048ALTKEY2".IX2114.2
034000     05 FILLER PIC X(53) VALUE                                    IX2114.2
034100          "RRRSSSSSSS354VVVWWWWWWW354ALTKEY1GGGFFFFFFF046ALTKEY2".IX2114.2
034200     05 FILLER PIC X(53) VALUE                                    IX2114.2
034300          "RRSSSSSSSS356VVWWWWWWWW356ALTKEY1GGFFFFFFFF044ALTKEY2".IX2114.2
034400     05 FILLER PIC X(53) VALUE                                    IX2114.2
034500          "RSSSSSSSSS358VWWWWWWWWW358ALTKEY1GFFFFFFFFF042ALTKEY2".IX2114.2
034600     05 FILLER PIC X(53) VALUE                                    IX2114.2
034700          "SSSSSSSSSS360WWWWWWWWWW360ALTKEY1FFFFFFFFFF040ALTKEY2".IX2114.2
034800     05 FILLER PIC X(53) VALUE                                    IX2114.2
034900          "SSSSSSSSST362WWWWWWWWWX362ALTKEY1FFFFFFFFFF040ALTKEY2".IX2114.2
035000     05 FILLER PIC X(53) VALUE                                    IX2114.2
035100          "SSSSSSSSTT364WWWWWWWWXX364ALTKEY1FFFFFFFFEE036ALTKEY2".IX2114.2
035200     05 FILLER PIC X(53) VALUE                                    IX2114.2
035300          "SSSSSSSTTT366WWWWWWWXXX366ALTKEY1FFFFFFFEEE034ALTKEY2".IX2114.2
035400     05 FILLER PIC X(53) VALUE                                    IX2114.2
035500          "SSSSSSTTTT368WWWWWWXXXX368ALTKEY1FFFFFFEEEE032ALTKEY2".IX2114.2
035600     05 FILLER PIC X(53) VALUE                                    IX2114.2
035700          "SSSSSTTTTT370WWWWWXXXXX370ALTKEY1FFFFFEEEEE030ALTKEY2".IX2114.2
035800     05 FILLER PIC X(53) VALUE                                    IX2114.2
035900          "SSSSTTTTTT372WWWWXXXXXX372ALTKEY1FFFFEEEEEE028ALTKEY2".IX2114.2
036000     05 FILLER PIC X(53) VALUE                                    IX2114.2
036100          "SSSTTTTTTT374WWWXXXXXXX374ALTKEY1FFFEEEEEEE026ALTKEY2".IX2114.2
036200     05 FILLER PIC X(53) VALUE                                    IX2114.2
036300          "SSTTTTTTTT376WWXXXXXXXX376ALTKEY1FFEEEEEEEE024ALTKEY2".IX2114.2
036400     05 FILLER PIC X(53) VALUE                                    IX2114.2
036500          "STTTTTTTTT378WXXXXXXXXX378ALTKEY1FEEEEEEEEE022ALTKEY2".IX2114.2
036600     05 FILLER PIC X(53) VALUE                                    IX2114.2
036700          "TTTTTTTTTT380XXXXXXXXXX380ALTKEY1EEEEEEEEEE020ALTKEY2".IX2114.2
036800     05 FILLER PIC X(53) VALUE                                    IX2114.2
036900          "TTTTTTTTTU382XXXXXXXXXY382ALTKEY1EEEEEEEEEE020ALTKEY2".IX2114.2
037000     05 FILLER PIC X(53) VALUE                                    IX2114.2
037100          "TTTTTTTTUU384XXXXXXXXYY384ALTKEY1EEEEEEEEDD016ALTKEY2".IX2114.2
037200     05 FILLER PIC X(53) VALUE                                    IX2114.2
037300          "TTTTTTTUUU386XXXXXXXYYY386ALTKEY1EEEEEEEDDD014ALTKEY2".IX2114.2
037400     05 FILLER PIC X(53) VALUE                                    IX2114.2
037500          "TTTTTTUUUU388XXXXXXYYYY388ALTKEY1EEEEEEDDDD012ALTKEY2".IX2114.2
037600     05 FILLER PIC X(53) VALUE                                    IX2114.2
037700          "TTTTTUUUUU390XXXXXYYYYY390ALTKEY1EEEEEDDDDD010ALTKEY2".IX2114.2
037800     05 FILLER PIC X(53) VALUE                                    IX2114.2
037900          "TTTTUUUUUU392XXXXYYYYYY392ALTKEY1EEEEDDDDDD008ALTKEY2".IX2114.2
038000     05 FILLER PIC X(53) VALUE                                    IX2114.2
038100          "TTTUUUUUUU394XXXYYYYYYY394ALTKEY1EEEDDDDDDD006ALTKEY2".IX2114.2
038200     05 FILLER PIC X(53) VALUE                                    IX2114.2
038300          "TTUUUUUUUU396XXYYYYYYYY396ALTKEY1EEDDDDDDDD004ALTKEY2".IX2114.2
038400     05 FILLER PIC X(53) VALUE                                    IX2114.2
038500          "TUUUUUUUUU398XYYYYYYYYY398ALTKEY1EDDDDDDDDD002ALTKEY2".IX2114.2
038600     05 FILLER PIC X(53) VALUE                                    IX2114.2
038700          "UUUUUUUUUU400YYYYYYYYYY400ALTKEY1DDDDDDDDDD000ALTKEY2".IX2114.2
038800 01  RECORD-KEY-DATA   REDEFINES  RECORD-KEY-CONTENT.             IX2114.2
038900     05 KEY-VALUES                  OCCURS 75  TIMES.             IX2114.2
039000       10 RECKEY-VALUE           PICTURE X(13).                   IX2114.2
039100       10 ALTKEY1-VALUE          PICTURE X(20).                   IX2114.2
039200       10 ALTKEY2-VALUE          PICTURE X(20).                   IX2114.2
039300 01  INIT-FLAG                   PICTURE 9.                       IX2114.2
039400 01  HOLD-FILESTATUS-RECORD.                                      IX2114.2
039500     05 FILESTATUS               PICTURE XX  OCCURS 10 TIMES.     IX2114.2
039600 01  FILE-RECORD-INFORMATION-REC.                                 IX2114.2
039700     03 FILE-RECORD-INFO-SKELETON.                                IX2114.2
039800        05 FILLER                 PICTURE X(48)       VALUE       IX2114.2
039900             "FILE=      ,RECORD=      /0,RECNO=000000,UPDT=00".  IX2114.2
040000        05 FILLER                 PICTURE X(46)       VALUE       IX2114.2
040100             ",ODO=0000,PGM=     ,LRECL=000000,BLKSIZ  =0000".    IX2114.2
040200        05 FILLER                 PICTURE X(26)       VALUE       IX2114.2
040300             ",LFIL=000000,ORG=  ,LBLR= ".                        IX2114.2
040400        05 FILLER                 PICTURE X(37)       VALUE       IX2114.2
040500             ",RECKEY=                             ".             IX2114.2
040600        05 FILLER                 PICTURE X(38)       VALUE       IX2114.2
040700             ",ALTKEY1=                             ".            IX2114.2
040800        05 FILLER                 PICTURE X(38)       VALUE       IX2114.2
040900             ",ALTKEY2=                             ".            IX2114.2
041000        05 FILLER                 PICTURE X(7)        VALUE SPACE.IX2114.2
041100     03 FILE-RECORD-INFO          OCCURS  10  TIMES.              IX2114.2
041200        05 FILE-RECORD-INFO-P1-120.                               IX2114.2
041300           07 FILLER              PIC X(5).                       IX2114.2
041400           07 XFILE-NAME           PIC X(6).                      IX2114.2
041500           07 FILLER              PIC X(8).                       IX2114.2
041600           07 XRECORD-NAME         PIC X(6).                      IX2114.2
041700           07 FILLER              PIC X(1).                       IX2114.2
041800           07 REELUNIT-NUMBER     PIC 9(1).                       IX2114.2
041900           07 FILLER              PIC X(7).                       IX2114.2
042000           07 XRECORD-NUMBER       PIC 9(6).                      IX2114.2
042100           07 FILLER              PIC X(6).                       IX2114.2
042200           07 UPDATE-NUMBER       PIC 9(2).                       IX2114.2
042300           07 FILLER              PIC X(5).                       IX2114.2
042400           07 ODO-NUMBER          PIC 9(4).                       IX2114.2
042500           07 FILLER              PIC X(5).                       IX2114.2
042600           07 XPROGRAM-NAME        PIC X(5).                      IX2114.2
042700           07 FILLER              PIC X(7).                       IX2114.2
042800           07 XRECORD-LENGTH       PIC 9(6).                      IX2114.2
042900           07 FILLER              PIC X(7).                       IX2114.2
043000           07 CHARS-OR-RECORDS    PIC X(2).                       IX2114.2
043100           07 FILLER              PIC X(1).                       IX2114.2
043200           07 XBLOCK-SIZE          PIC 9(4).                      IX2114.2
043300           07 FILLER              PIC X(6).                       IX2114.2
043400           07 RECORDS-IN-FILE     PIC 9(6).                       IX2114.2
043500           07 FILLER              PIC X(5).                       IX2114.2
043600           07 XFILE-ORGANIZATION   PIC X(2).                      IX2114.2
043700           07 FILLER              PIC X(6).                       IX2114.2
043800           07 XLABEL-TYPE          PIC X(1).                      IX2114.2
043900        05 FILE-RECORD-INFO-P121-240.                             IX2114.2
044000           07 FILLER              PIC X(8).                       IX2114.2
044100           07 XRECORD-KEY          PIC X(29).                     IX2114.2
044200           07 FILLER              PIC X(9).                       IX2114.2
044300           07 ALTERNATE-KEY1      PIC X(29).                      IX2114.2
044400           07 FILLER              PIC X(9).                       IX2114.2
044500           07 ALTERNATE-KEY2      PIC X(29).                      IX2114.2
044600           07 FILLER              PIC X(7).                       IX2114.2
044700 01  TEST-RESULTS.                                                IX2114.2
044800     02 FILLER                   PIC X      VALUE SPACE.          IX2114.2
044900     02 FEATURE                  PIC X(20)  VALUE SPACE.          IX2114.2
045000     02 FILLER                   PIC X      VALUE SPACE.          IX2114.2
045100     02 P-OR-F                   PIC X(5)   VALUE SPACE.          IX2114.2
045200     02 FILLER                   PIC X      VALUE SPACE.          IX2114.2
045300     02  PAR-NAME.                                                IX2114.2
045400       03 FILLER                 PIC X(19)  VALUE SPACE.          IX2114.2
045500       03  PARDOT-X              PIC X      VALUE SPACE.          IX2114.2
045600       03 DOTVALUE               PIC 99     VALUE ZERO.           IX2114.2
045700     02 FILLER                   PIC X(8)   VALUE SPACE.          IX2114.2
045800     02 RE-MARK                  PIC X(61).                       IX2114.2
045900 01  TEST-COMPUTED.                                               IX2114.2
046000     02 FILLER                   PIC X(30)  VALUE SPACE.          IX2114.2
046100     02 FILLER                   PIC X(17)  VALUE                 IX2114.2
046200            "       COMPUTED=".                                   IX2114.2
046300     02 COMPUTED-X.                                               IX2114.2
046400     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          IX2114.2
046500     03 COMPUTED-N               REDEFINES COMPUTED-A             IX2114.2
046600                                 PIC -9(9).9(9).                  IX2114.2
046700     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         IX2114.2
046800     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     IX2114.2
046900     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     IX2114.2
047000     03       CM-18V0 REDEFINES COMPUTED-A.                       IX2114.2
047100         04 COMPUTED-18V0                    PIC -9(18).          IX2114.2
047200         04 FILLER                           PIC X.               IX2114.2
047300     03 FILLER PIC X(50) VALUE SPACE.                             IX2114.2
047400 01  TEST-CORRECT.                                                IX2114.2
047500     02 FILLER PIC X(30) VALUE SPACE.                             IX2114.2
047600     02 FILLER PIC X(17) VALUE "       CORRECT =".                IX2114.2
047700     02 CORRECT-X.                                                IX2114.2
047800     03 CORRECT-A                  PIC X(20) VALUE SPACE.         IX2114.2
047900     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      IX2114.2
048000     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         IX2114.2
048100     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     IX2114.2
048200     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     IX2114.2
048300     03      CR-18V0 REDEFINES CORRECT-A.                         IX2114.2
048400         04 CORRECT-18V0                     PIC -9(18).          IX2114.2
048500         04 FILLER                           PIC X.               IX2114.2
048600     03 FILLER PIC X(2) VALUE SPACE.                              IX2114.2
048700     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     IX2114.2
048800 01  CCVS-C-1.                                                    IX2114.2
048900     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PAIX2114.2
049000-    "SS  PARAGRAPH-NAME                                          IX2114.2
049100-    "       REMARKS".                                            IX2114.2
049200     02 FILLER                     PIC X(20)    VALUE SPACE.      IX2114.2
049300 01  CCVS-C-2.                                                    IX2114.2
049400     02 FILLER                     PIC X        VALUE SPACE.      IX2114.2
049500     02 FILLER                     PIC X(6)     VALUE "TESTED".   IX2114.2
049600     02 FILLER                     PIC X(15)    VALUE SPACE.      IX2114.2
049700     02 FILLER                     PIC X(4)     VALUE "FAIL".     IX2114.2
049800     02 FILLER                     PIC X(94)    VALUE SPACE.      IX2114.2
049900 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       IX2114.2
050000 01  REC-CT                        PIC 99       VALUE ZERO.       IX2114.2
050100 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       IX2114.2
050200 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       IX2114.2
050300 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       IX2114.2
050400 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       IX2114.2
050500 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       IX2114.2
050600 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       IX2114.2
050700 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      IX2114.2
050800 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       IX2114.2
050900 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     IX2114.2
051000 01  CCVS-H-1.                                                    IX2114.2
051100     02  FILLER                    PIC X(39)    VALUE SPACES.     IX2114.2
051200     02  FILLER                    PIC X(42)    VALUE             IX2114.2
051300     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 IX2114.2
051400     02  FILLER                    PIC X(39)    VALUE SPACES.     IX2114.2
051500 01  CCVS-H-2A.                                                   IX2114.2
051600   02  FILLER                        PIC X(40)  VALUE SPACE.      IX2114.2
051700   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  IX2114.2
051800   02  FILLER                        PIC XXXX   VALUE             IX2114.2
051900     "4.2 ".                                                      IX2114.2
052000   02  FILLER                        PIC X(28)  VALUE             IX2114.2
052100            " COPY - NOT FOR DISTRIBUTION".                       IX2114.2
052200   02  FILLER                        PIC X(41)  VALUE SPACE.      IX2114.2
052300                                                                  IX2114.2
052400 01  CCVS-H-2B.                                                   IX2114.2
052500   02  FILLER                        PIC X(15)  VALUE             IX2114.2
052600            "TEST RESULT OF ".                                    IX2114.2
052700   02  TEST-ID                       PIC X(9).                    IX2114.2
052800   02  FILLER                        PIC X(4)   VALUE             IX2114.2
052900            " IN ".                                               IX2114.2
053000   02  FILLER                        PIC X(12)  VALUE             IX2114.2
053100     " HIGH       ".                                              IX2114.2
053200   02  FILLER                        PIC X(22)  VALUE             IX2114.2
053300            " LEVEL VALIDATION FOR ".                             IX2114.2
053400   02  FILLER                        PIC X(58)  VALUE             IX2114.2
053500     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IX2114.2
053600 01  CCVS-H-3.                                                    IX2114.2
053700     02  FILLER                      PIC X(34)  VALUE             IX2114.2
053800            " FOR OFFICIAL USE ONLY    ".                         IX2114.2
053900     02  FILLER                      PIC X(58)  VALUE             IX2114.2
054000     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".IX2114.2
054100     02  FILLER                      PIC X(28)  VALUE             IX2114.2
054200            "  COPYRIGHT   1985 ".                                IX2114.2
054300 01  CCVS-E-1.                                                    IX2114.2
054400     02 FILLER                       PIC X(52)  VALUE SPACE.      IX2114.2
054500     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              IX2114.2
054600     02 ID-AGAIN                     PIC X(9).                    IX2114.2
054700     02 FILLER                       PIC X(45)  VALUE SPACES.     IX2114.2
054800 01  CCVS-E-2.                                                    IX2114.2
054900     02  FILLER                      PIC X(31)  VALUE SPACE.      IX2114.2
055000     02  FILLER                      PIC X(21)  VALUE SPACE.      IX2114.2
055100     02 CCVS-E-2-2.                                               IX2114.2
055200         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      IX2114.2
055300         03 FILLER                   PIC X      VALUE SPACE.      IX2114.2
055400         03 ENDER-DESC               PIC X(44)  VALUE             IX2114.2
055500            "ERRORS ENCOUNTERED".                                 IX2114.2
055600 01  CCVS-E-3.                                                    IX2114.2
055700     02  FILLER                      PIC X(22)  VALUE             IX2114.2
055800            " FOR OFFICIAL USE ONLY".                             IX2114.2
055900     02  FILLER                      PIC X(12)  VALUE SPACE.      IX2114.2
056000     02  FILLER                      PIC X(58)  VALUE             IX2114.2
056100     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IX2114.2
056200     02  FILLER                      PIC X(13)  VALUE SPACE.      IX2114.2
056300     02 FILLER                       PIC X(15)  VALUE             IX2114.2
056400             " COPYRIGHT 1985".                                   IX2114.2
056500 01  CCVS-E-4.                                                    IX2114.2
056600     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      IX2114.2
056700     02 FILLER                       PIC X(4)   VALUE " OF ".     IX2114.2
056800     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      IX2114.2
056900     02 FILLER                       PIC X(40)  VALUE             IX2114.2
057000      "  TESTS WERE EXECUTED SUCCESSFULLY".                       IX2114.2
057100 01  XXINFO.                                                      IX2114.2
057200     02 FILLER                       PIC X(19)  VALUE             IX2114.2
057300            "*** INFORMATION ***".                                IX2114.2
057400     02 INFO-TEXT.                                                IX2114.2
057500       04 FILLER                     PIC X(8)   VALUE SPACE.      IX2114.2
057600       04 XXCOMPUTED                 PIC X(20).                   IX2114.2
057700       04 FILLER                     PIC X(5)   VALUE SPACE.      IX2114.2
057800       04 XXCORRECT                  PIC X(20).                   IX2114.2
057900     02 INF-ANSI-REFERENCE           PIC X(48).                   IX2114.2
058000 01  HYPHEN-LINE.                                                 IX2114.2
058100     02 FILLER  PIC IS X VALUE IS SPACE.                          IX2114.2
058200     02 FILLER  PIC IS X(65)    VALUE IS "************************IX2114.2
058300-    "*****************************************".                 IX2114.2
058400     02 FILLER  PIC IS X(54)    VALUE IS "************************IX2114.2
058500-    "******************************".                            IX2114.2
058600 01  CCVS-PGM-ID                     PIC X(9)   VALUE             IX2114.2
058700     "IX211A".                                                    IX2114.2
058800 PROCEDURE DIVISION.                                              IX2114.2
058900 CCVS1 SECTION.                                                   IX2114.2
059000 OPEN-FILES.                                                      IX2114.2
059100*    OPEN I-O RAW-DATA.                                           IX2114.2
059200*    MOVE CCVS-PGM-ID TO RAW-DATA-KEY.                            IX2114.2
059300*    READ RAW-DATA INVALID KEY GO TO END-E-1.                     IX2114.2
059400*    MOVE "ABORTED " TO C-ABORT.                                  IX2114.2
059500*    ADD 1 TO C-NO-OF-TESTS.                                      IX2114.2
059600*    ACCEPT C-DATE  FROM DATE.                                    IX2114.2
059700*    ACCEPT C-TIME  FROM TIME.                                    IX2114.2
059800*    REWRITE RAW-DATA-SATZ INVALID KEY GO TO END-E-1.             IX2114.2
059900*END-E-1.                                                         IX2114.2
060000*    CLOSE RAW-DATA.                                              IX2114.2
060100     OPEN    OUTPUT PRINT-FILE.                                   IX2114.2
060200     MOVE  CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.  IX2114.2
060300     MOVE    SPACE TO TEST-RESULTS.                               IX2114.2
060400     PERFORM HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.              IX2114.2
060500     MOVE    ZERO TO REC-SKL-SUB.                                 IX2114.2
060600     PERFORM CCVS-INIT-FILE 9 TIMES.                              IX2114.2
060700 CCVS-INIT-FILE.                                                  IX2114.2
060800     ADD     1 TO REC-SKL-SUB.                                    IX2114.2
060900     MOVE    FILE-RECORD-INFO-SKELETON                            IX2114.2
061000          TO FILE-RECORD-INFO (REC-SKL-SUB).                      IX2114.2
061100 CCVS-INIT-EXIT.                                                  IX2114.2
061200     GO TO CCVS1-EXIT.                                            IX2114.2
061300 CLOSE-FILES.                                                     IX2114.2
061400*    OPEN I-O RAW-DATA.                                           IX2114.2
061500*    MOVE CCVS-PGM-ID TO RAW-DATA-KEY.                            IX2114.2
061600*    READ RAW-DATA INVALID KEY GO TO END-E-2.                     IX2114.2
061700*    MOVE "OK.     " TO C-ABORT.                                  IX2114.2
061800*    MOVE PASS-COUNTER TO C-OK.                                   IX2114.2
061900*    MOVE ERROR-HOLD   TO C-ALL.                                  IX2114.2
062000*    MOVE ERROR-COUNTER TO C-FAIL.                                IX2114.2
062100*    MOVE DELETE-COUNTER TO C-DELETED.                            IX2114.2
062200*    MOVE INSPECT-COUNTER TO C-INSPECT.                           IX2114.2
062300*    REWRITE RAW-DATA-SATZ INVALID KEY GO TO END-E-2.             IX2114.2
062400*END-E-2.                                                         IX2114.2
062500*    CLOSE RAW-DATA.                                              IX2114.2
062600     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   IX2114.2
062700 TERMINATE-CCVS.                                                  IX2114.2
062800*    EXIT PROGRAM.                                                IX2114.2
062900*TERMINATE-CALL.                                                  IX2114.2
063000     STOP     RUN.                                                IX2114.2
063100 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         IX2114.2
063200 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           IX2114.2
063300 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          IX2114.2
063400 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      IX2114.2
063500     MOVE "****TEST DELETED****" TO RE-MARK.                      IX2114.2
063600 PRINT-DETAIL.                                                    IX2114.2
063700     IF REC-CT NOT EQUAL TO ZERO                                  IX2114.2
063800             MOVE "." TO PARDOT-X                                 IX2114.2
063900             MOVE REC-CT TO DOTVALUE.                             IX2114.2
064000     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      IX2114.2
064100     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               IX2114.2
064200        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 IX2114.2
064300          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 IX2114.2
064400     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              IX2114.2
064500     MOVE SPACE TO CORRECT-X.                                     IX2114.2
064600     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         IX2114.2
064700     MOVE     SPACE TO RE-MARK.                                   IX2114.2
064800 HEAD-ROUTINE.                                                    IX2114.2
064900     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  IX2114.2
065000     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  IX2114.2
065100     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  IX2114.2
065200     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  IX2114.2
065300 COLUMN-NAMES-ROUTINE.                                            IX2114.2
065400     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IX2114.2
065500     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IX2114.2
065600     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        IX2114.2
065700 END-ROUTINE.                                                     IX2114.2
065800     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.IX2114.2
065900 END-RTN-EXIT.                                                    IX2114.2
066000     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IX2114.2
066100 END-ROUTINE-1.                                                   IX2114.2
066200      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      IX2114.2
066300      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               IX2114.2
066400      ADD PASS-COUNTER TO ERROR-HOLD.                             IX2114.2
066500*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   IX2114.2
066600      MOVE PASS-COUNTER TO CCVS-E-4-1.                            IX2114.2
066700      MOVE ERROR-HOLD TO CCVS-E-4-2.                              IX2114.2
066800      MOVE CCVS-E-4 TO CCVS-E-2-2.                                IX2114.2
066900      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           IX2114.2
067000  END-ROUTINE-12.                                                 IX2114.2
067100      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        IX2114.2
067200     IF       ERROR-COUNTER IS EQUAL TO ZERO                      IX2114.2
067300         MOVE "NO " TO ERROR-TOTAL                                IX2114.2
067400         ELSE                                                     IX2114.2
067500         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       IX2114.2
067600     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           IX2114.2
067700     PERFORM WRITE-LINE.                                          IX2114.2
067800 END-ROUTINE-13.                                                  IX2114.2
067900     IF DELETE-COUNTER IS EQUAL TO ZERO                           IX2114.2
068000         MOVE "NO " TO ERROR-TOTAL  ELSE                          IX2114.2
068100         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      IX2114.2
068200     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   IX2114.2
068300     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IX2114.2
068400      IF   INSPECT-COUNTER EQUAL TO ZERO                          IX2114.2
068500          MOVE "NO " TO ERROR-TOTAL                               IX2114.2
068600      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   IX2114.2
068700      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            IX2114.2
068800      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          IX2114.2
068900     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IX2114.2
069000 WRITE-LINE.                                                      IX2114.2
069100     ADD 1 TO RECORD-COUNT.                                       IX2114.2
069200*    IF RECORD-COUNT GREATER 42                                   IX2114.2
069300*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          IX2114.2
069400*        MOVE SPACE TO DUMMY-RECORD                               IX2114.2
069500*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  IX2114.2
069600*        MOVE CCVS-H-1  TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES    IX2114.2
069700*        MOVE CCVS-H-2A TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES    IX2114.2
069800*        MOVE CCVS-H-2B TO DUMMY-RECORD PERFORM WRT-LN 3 TIMES    IX2114.2
069900*        MOVE CCVS-H-3  TO DUMMY-RECORD PERFORM WRT-LN 3 TIMES    IX2114.2
070000*        MOVE CCVS-C-1  TO DUMMY-RECORD PERFORM WRT-LN            IX2114.2
070100*        MOVE CCVS-C-2  TO DUMMY-RECORD PERFORM WRT-LN            IX2114.2
070200*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          IX2114.2
070300*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          IX2114.2
070400*        MOVE ZERO TO RECORD-COUNT.                               IX2114.2
070500     PERFORM WRT-LN.                                              IX2114.2
070600 WRT-LN.                                                          IX2114.2
070700     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               IX2114.2
070800     MOVE SPACE TO DUMMY-RECORD.                                  IX2114.2
070900 BLANK-LINE-PRINT.                                                IX2114.2
071000     PERFORM WRT-LN.                                              IX2114.2
071100 FAIL-ROUTINE.                                                    IX2114.2
071200     IF     COMPUTED-X NOT EQUAL TO SPACE                         IX2114.2
071300            GO TO   FAIL-ROUTINE-WRITE.                           IX2114.2
071400     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.IX2114.2
071500     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 IX2114.2
071600     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   IX2114.2
071700     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IX2114.2
071800     MOVE   SPACES TO INF-ANSI-REFERENCE.                         IX2114.2
071900     GO TO  FAIL-ROUTINE-EX.                                      IX2114.2
072000 FAIL-ROUTINE-WRITE.                                              IX2114.2
072100     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         IX2114.2
072200     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 IX2114.2
072300     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. IX2114.2
072400     MOVE   SPACES TO COR-ANSI-REFERENCE.                         IX2114.2
072500 FAIL-ROUTINE-EX. EXIT.                                           IX2114.2
072600 BAIL-OUT.                                                        IX2114.2
072700     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   IX2114.2
072800     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           IX2114.2
072900 BAIL-OUT-WRITE.                                                  IX2114.2
073000     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  IX2114.2
073100     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 IX2114.2
073200     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IX2114.2
073300     MOVE   SPACES TO INF-ANSI-REFERENCE.                         IX2114.2
073400 BAIL-OUT-EX. EXIT.                                               IX2114.2
073500 CCVS1-EXIT.                                                      IX2114.2
073600     EXIT.                                                        IX2114.2
073700 SECT-0001-IX211A SECTION.                                        IX2114.2
073800 WRITE-INT-GF-01.                                                 IX2114.2
073900     OPEN    OUTPUT IX-FD1.                                       IX2114.2
074000     MOVE    "IX-FD1" TO XFILE-NAME           (1).                IX2114.2
074100     MOVE    "R1-F-G" TO  XRECORD-NAME        (1).                IX2114.2
074200     MOVE    ZERO     TO  XRECORD-NUMBER      (1).                IX2114.2
074300     MOVE    "RC"     TO  CHARS-OR-RECORDS    (1).                IX2114.2
074400     MOVE    CCVS-PGM-ID TO  XPROGRAM-NAME       (1).             IX2114.2
074500     MOVE    240      TO  XRECORD-LENGTH      (1).                IX2114.2
074600     MOVE    001      TO  XBLOCK-SIZE         (1).                IX2114.2
074700     MOVE    "IX"     TO  XFILE-ORGANIZATION  (1).                IX2114.2
074800     MOVE    "S"      TO  XLABEL-TYPE         (1).                IX2114.2
074900     MOVE    200      TO  RECORDS-IN-FILE     (1).                IX2114.2
075000     MOVE    "CREATE-FILE-FD1" TO FEATURE.                        IX2114.2
075100     MOVE    "WRITE-TEST-GF-01" TO PAR-NAME.                      IX2114.2
075200     MOVE    ZERO TO KEYSUB.                                      IX2114.2
075300     MOVE    ZERO TO INVKEY-COUNTER.                              IX2114.2
075400 WRITE-INIT-GF-01-01.                                             IX2114.2
075500     PERFORM WRITE-TEST-GF-01-1  50 TIMES.                        IX2114.2
075600     PERFORM WRITE-TEST-GF-01-2  125 TIMES.                       IX2114.2
075700     PERFORM WRITE-TEST-GF-01-1  25 TIMES.                        IX2114.2
075800     GO TO   WRITE-TEST-GF-01.                                    IX2114.2
075900 WRITE-TEST-GF-01-1.                                              IX2114.2
076000     ADD     001  TO XRECORD-NUMBER (1).                          IX2114.2
076100     ADD     001  TO  KEYSUB.                                     IX2114.2
076200     MOVE    RECKEY-VALUE (KEYSUB) TO FD1-RECKEY-1-13.            IX2114.2
076300     MOVE    ALTKEY1-VALUE (KEYSUB) TO FD1-ALTKEY1-1-20.          IX2114.2
076400     MOVE    ALTKEY2-VALUE (KEYSUB) TO FD1-ALTKEY2-1-20.          IX2114.2
076500     MOVE    WRK-FD1-RECKEY   TO    XRECORD-KEY (1).              IX2114.2
076600     MOVE    WRK-FD1-ALTKEY1  TO ALTERNATE-KEY1 (1).              IX2114.2
076700     MOVE    WRK-FD1-ALTKEY2  TO ALTERNATE-KEY2 (1).              IX2114.2
076800     MOVE    FILE-RECORD-INFO (1)  TO IX-FD1R1-F-G-240.           IX2114.2
076900     WRITE   IX-FD1R1-F-G-240                                     IX2114.2
077000                        INVALID KEY ADD 001 TO INVKEY-COUNTER.    IX2114.2
077100     ADD     001 TO  EXCUT-COUNTER-06V00.                         IX2114.2
077200 WRITE-TEST-GF-01-2.                                              IX2114.2
077300     ADD     002  TO FD1-RECKEY-11-13.                            IX2114.2
077400     ADD     002 TO FD1-ALTKEY1-11-13.                            IX2114.2
077500     SUBTRACT  002 FROM FD1-ALTKEY2-11-13.                        IX2114.2
077600     ADD     001  TO XRECORD-NUMBER (1).                          IX2114.2
077700     MOVE    WRK-FD1-RECKEY   TO    XRECORD-KEY (1).              IX2114.2
077800     MOVE    WRK-FD1-ALTKEY1  TO ALTERNATE-KEY1 (1).              IX2114.2
077900     MOVE    WRK-FD1-ALTKEY2  TO ALTERNATE-KEY2 (1).              IX2114.2
078000     MOVE    FILE-RECORD-INFO (1) TO IX-FD1R1-F-G-240.            IX2114.2
078100     WRITE   IX-FD1R1-F-G-240                                     IX2114.2
078200                        INVALID KEY ADD 001 TO INVKEY-COUNTER.    IX2114.2
078300     ADD     001 TO EXCUT-COUNTER-06V00.                          IX2114.2
078400 WRITE-TEST-GF-01.                                                IX2114.2
078500     SUBTRACT INVKEY-COUNTER FROM EXCUT-COUNTER-06V00             IX2114.2
078600             GIVING RECORDS-WRITTEN.                              IX2114.2
078700     MOVE    200  TO CORRECT-18V0.                                IX2114.2
078800     MOVE    RECORDS-WRITTEN TO COMPUTED-18V0.                    IX2114.2
078900     IF      RECORDS-WRITTEN EQUAL TO 200                         IX2114.2
079000             PERFORM  PASS                                        IX2114.2
079100             ELSE                                                 IX2114.2
079200             PERFORM FAIL.                                        IX2114.2
079300     MOVE    "RECORDS IN FILE" TO RE-MARK.                        IX2114.2
079400     GO TO   WRITE-TEST-GF-01-END.                                IX2114.2
079500 WRITE-DELETE-GF-01.                                              IX2114.2
079600     PERFORM DE-LETE.                                             IX2114.2
079700 WRITE-TEST-GF-01-END.                                            IX2114.2
079800     PERFORM  PRINT-DETAIL.                                       IX2114.2
079900     CLOSE   IX-FD1.                                              IX2114.2
080000 READ-INIT-F1-01.                                                 IX2114.2
080100     OPEN    INPUT  IX-FD1.                                       IX2114.2
080200     MOVE    "READ-TEST-F1-01" TO PAR-NAME.                       IX2114.2
080300     MOVE    "READ FILE IX-FD1" TO FEATURE.                       IX2114.2
080400     MOVE    ZERO TO EXCUT-COUNTER-06V00.                         IX2114.2
080500     MOVE    02   TO RECKEY-NUM.                                  IX2114.2
080600     MOVE    002 TO ALTKEY1-NUM.                                  IX2114.2
080700 READ-TEST-F1-01-1.                                               IX2114.2
080800     READ     IX-FD1 NEXT                                         IX2114.2
080900             AT END GO TO READ-TEST-F1-01.                        IX2114.2
081000     MOVE    IX-REC-KEY-AREA TO WRK-FD1-RECKEY.                   IX2114.2
081100     MOVE    IX-ALT-KEY1-AREA TO WRK-FD1-ALTKEY1.                 IX2114.2
081200     IF      FD1-RECKEY-11-13 EQUAL TO RECKEY-NUM                 IX2114.2
081300             AND FD1-ALTKEY1-11-13 EQUAL TO ALTKEY1-NUM           IX2114.2
081400             NEXT SENTENCE                                        IX2114.2
081500             ELSE                                                 IX2114.2
081600             PERFORM  READ-FAIL-F1-01.                            IX2114.2
081700     IF      EXCUT-COUNTER-06V00 GREATER THAN 200                 IX2114.2
081800             GO TO READ-TEST-F1-01.                               IX2114.2
081900     ADD     001  TO EXCUT-COUNTER-06V00.                         IX2114.2
082000     ADD     002  TO RECKEY-NUM                                   IX2114.2
082100     ADD     002  TO ALTKEY1-NUM.                                 IX2114.2
082200     GO TO   READ-TEST-F1-01-1.                                   IX2114.2
082300 READ-TEST-F1-01.                                                 IX2114.2
082400     IF      EXCUT-COUNTER-06V00 EQUAL TO 200                     IX2114.2
082500             PERFORM PASS ELSE                                    IX2114.2
082600             PERFORM FAIL.                                        IX2114.2
082700     MOVE    EXCUT-COUNTER-06V00 TO  COMPUTED-18V0.               IX2114.2
082800     MOVE    200  TO  CORRECT-18V0.                               IX2114.2
082900     MOVE    "RECORDS IN FILE" TO RE-MARK.                        IX2114.2
083000     GO TO   READ-EXIT-F1-01.                                     IX2114.2
083100 READ-FAIL-F1-01.                                                 IX2114.2
083200     PERFORM FAIL.                                                IX2114.2
083300     MOVE    FD1-RECKEY-11-13  TO COMPUTED-18V0.                  IX2114.2
083400     MOVE    RECKEY-NUM  TO CORRECT-18V0.                         IX2114.2
083500     MOVE "NUM EMBEDDED IN RECKEY; IX-28 READ; IX-41 WRITE"       IX2114.2
083600              TO RE-MARK.                                         IX2114.2
083700 READ-EXIT-F1-01.                                                 IX2114.2
083800     PERFORM PRINT-DETAIL.                                        IX2114.2
083900     CLOSE   IX-FD1.                                              IX2114.2
084000 READ-INIT-F1-02.                                                 IX2114.2
084100     OPEN I-O  IX-FD1.                                            IX2114.2
084200     MOVE "START & READ NEXT   " TO FEATURE.                      IX2114.2
084300     MOVE "READ-TEST-F1-02     " TO PAR-NAME.                     IX2114.2
084400     MOVE    "SSSSSSSSTT364"  TO FD1-RECKEY-1-13.                 IX2114.2
084500     MOVE    "WWWWWWWWXX364ALTKEY1"  TO FD1-ALTKEY1-1-20.         IX2114.2
084600     MOVE    "FFFFFFFFEE036ALTKEY2"  TO FD1-ALTKEY2-1-20.         IX2114.2
084700     MOVE    WRK-FD1-RECKEY TO IX-REC-KEY-AREA.                   IX2114.2
084800     MOVE    WRK-FD1-ALTKEY1 TO IX-ALT-KEY1-AREA.                 IX2114.2
084900     MOVE    WRK-FD1-ALTKEY2 TO IX-ALT-KEY2-AREA.                 IX2114.2
085000 READ-TEST-F1-02.                                                 IX2114.2
085100*                                                                 IX2114.2
085200*  READ-TEST-F1-02 - THIS TEST READS A RECORD AND CHECKS THE      IX2114.2
085300*                    RECORD MADE AVAILABLE.  THE PURPOSE IS TO    IX2114.2
085400*                    ESTABLISH IX-FD1-ALTKEY1 AS THE KEY OF REF-  IX2114.2
085500*                    ERENCE AND TO MAKE A RECORD AVAILABLE        IX2114.2
085600*                    FOR TESTING THE REWRITE STATEMENT IN THE     IX2114.2
085700*                    NEXT TEST.         RECORD 182 (ALTERNATE KEY IX2114.2
085800*                    "WWWWWWWWXX364ALTKEY1") IS EXPECTED TO BE    IX2114.2
085900*                    RETRIEVED.                                   IX2114.2
086000*                                                                 IX2114.2
086100     START   IX-FD1                                               IX2114.2
086200                    KEY IS EQUAL TO IX-FD1-ALTKEY1                IX2114.2
086300          INVALID KEY MOVE "INVALID KEY ON START" TO COMPUTED-A   IX2114.2
086400                     GO TO READ-FAIL-F1-02.                       IX2114.2
086500     READ    IX-FD1  NEXT  AT END                                 IX2114.2
086600             MOVE  "AT END ON READ"  TO COMPUTED-A                IX2114.2
086700             GO TO  READ-FAIL-F1-02.                              IX2114.2
086800     MOVE    IX-FD1R1-F-G-240  TO FILE-RECORD-INFO (1).           IX2114.2
086900     IF      XRECORD-NUMBER (1) EQUAL TO 182                      IX2114.2
087000             PERFORM   PASS                                       IX2114.2
087100             GO TO   READ-WRITE-F1-02.                            IX2114.2
087200 READ-FAIL-F1-02.                                                 IX2114.2
087300     PERFORM FAIL.                                                IX2114.2
087400     MOVE    "RECORD 182 RETRIEVED"  TO CORRECT-A.                IX2114.2
087500     MOVE "SEQUENTIAL READ; IX-28 4.5.2"  TO RE-MARK.             IX2114.2
087600 READ-WRITE-F1-02.                                                IX2114.2
087700     PERFORM PRINT-DETAIL.                                        IX2114.2
087800 RWRT-TEST-GF-01.                                                 IX2114.2
087900     MOVE "REWRITE             " TO FEATURE.                      IX2114.2
088000     MOVE "RWRT-TEST-GF-01     " TO PAR-NAME.                     IX2114.2
088100*                                                                 IX2114.2
088200*  RWRT-TEST-GF-01 - THE TEST MODIFIES THE CONTENTS OF ALTERNATE- IX2114.2
088300*                    KEY1 OF THE RECORD RETRIEVED BY THE TEST     IX2114.2
088400*                    BEFORE AND REWRITES THE RECORD.THE NEW ALTER-IX2114.2
088500*                    NATE KEY VALUE IS "WWWWWWXXXX369ALTKEY1"     IX2114.2
088600*                    WHICH BECOMES SEQUENTIAL RECORD NUMBER  184. IX2114.2
088700*                    THE NEW KEY FOR THE FILE HAS A KEY VALUE     IX2114.2
088800*                    SEQUENTIALLY GREATER THAN THE RECORD VALUE   IX2114.2
088900*                    RETRIEVED FROM RECORD. THE SEQUENTIAL RECORD IX2114.2
089000*                    RETRIEVED BEFORE THE REWRITE WAS NUMBER      IX2114.2
089100*                    182.  THE CURRENT RECORD POINTER FOR THE FILEIX2114.2
089200*                    IS NOT EXPECTED TO BE MODIFIED BY            IX2114.2
089300*                    EXECUTION OF THE REWRITE.                    IX2114.2
089400*                                                                 IX2114.2
089500     MOVE    182  TO ODO-NUMBER (1).                              IX2114.2
089600     ADD     01  TO UPDATE-NUMBER (1).                            IX2114.2
089700     MOVE    "WWWWWWXXXX369ALTKEY1"  TO FD1-ALTKEY1-1-20.         IX2114.2
089800     MOVE    WRK-FD1-ALTKEY1  TO ALTERNATE-KEY1 (1).              IX2114.2
089900     MOVE    FILE-RECORD-INFO (1) TO IX-FD1R1-F-G-240.            IX2114.2
090000     REWRITE IX-FD1R1-F-G-240  INVALID KEY                        IX2114.2
090100                     MOVE   "INVALID KEY REWRITE" TO COMPUTED-A   IX2114.2
090200                     GO TO   RWRT-FAIL-GF-01.                     IX2114.2
090300     PERFORM PASS.                                                IX2114.2
090400     GO TO   RWRT-WRITE-GF-01.                                    IX2114.2
090500 RWRT-FAIL-GF-01.                                                 IX2114.2
090600     PERFORM FAIL.                                                IX2114.2
090700     MOVE "IX-33 4.6.2       "  TO RE-MARK.                       IX2114.2
090800 RWRT-WRITE-GF-01.                                                IX2114.2
090900     PERFORM PRINT-DETAIL.                                        IX2114.2
091000 READ-INIT-F1-03.                                                 IX2114.2
091100     MOVE "READ NEXT AT END    " TO FEATURE.                      IX2114.2
091200     MOVE "READ-TEST-F1-03     " TO PAR-NAME.                     IX2114.2
091300 READ-TEST-F1-03.                                                 IX2114.2
091400*                                                                 IX2114.2
091500*  READ-TEST-F1-03 - THIS TEST PERFORMS A SEQUENTIAL READ AND     IX2114.2
091600*                    CHECKS THE RECORD MADE AVAILABLE.  THE KEY OFIX2114.2
091700*                    REFERENCE IS EXPECTED TO BE THAT ESTABLISHED IX2114.2
091800*                    BEFORE.              THE LOGICAL RECORD      IX2114.2
091900*                    RETRIEVED IS EXPECTED TO BE THAT RECORD THAT IX2114.2
092000*                    WOULD HAVE BEEN RETRIEVED HAD THE ALTERNATE  IX2114.2
092100*                    KEY NOT BEEN CHANGE BY THE REWRITE IN        IX2114.2
092200*                    RWRT-TEST-GF-01.                             IX2114.2
092300*                                                                 IX2114.2
092400     READ    IX-FD1  NEXT  AT END                                 IX2114.2
092500                     MOVE   "AT END ON READ"  TO COMPUTED-A       IX2114.2
092600             GO TO   READ-FAIL-F1-03.                             IX2114.2
092700     MOVE    IX-FD1R1-F-G-240  TO FILE-RECORD-INFO (1).           IX2114.2
092800     IF      XRECORD-NUMBER (1) EQUAL TO 183                      IX2114.2
092900             PERFORM  PASS                                        IX2114.2
093000             GO TO READ-WRITE-F1-03.                              IX2114.2
093100     MOVE    58  TO RECNO.                                        IX2114.2
093200     PERFORM DISPLAY-ALTERNATE-KEY1.                              IX2114.2
093300     MOVE    XRECORD-NUMBER (1) TO COMPUTED-18V0.                 IX2114.2
093400 READ-FAIL-F1-03.                                                 IX2114.2
093500      PERFORM FAIL.                                               IX2114.2
093600     MOVE    183  TO CORRECT-18V0.                                IX2114.2
093700     MOVE "RECORD NUMBER; IX-28"  TO RE-MARK.                     IX2114.2
093800 READ-WRITE-F1-03.                                                IX2114.2
093900     PERFORM PRINT-DETAIL.                                        IX2114.2
094000 READ-INIT-F1-04.                                                 IX2114.2
094100     MOVE "READ NEXT 4 RECS    " TO FEATURE.                      IX2114.2
094200     MOVE "READ-TEST-F1-04     " TO PAR-NAME.                     IX2114.2
094300     MOVE    ZERO TO EXCUT-COUNTER-06V00.                         IX2114.2
094400     MOVE    183  TO RECKEY-NUM.                                  IX2114.2
094500 READ-TEST-F1-04.                                                 IX2114.2
094600*                                                                 IX2114.2
094700*  READ-TEST-F1-04 - THE TEST SEQUENTIALLY READS THE NEXT 4 REC-  IX2114.2
094800*                    ORDS AND CHECKS THE RECORD NUMBER FIELD OF   IX2114.2
094900*                    EACH RECORD RETRIEVED.  THE CONTENTS OF      IX2114.2
095000*                    THE RECORD NUMBER FIELD IS EXPECTED TO BE    IX2114.2
095100*                    184, 182, 185 AND 186 (ALTERNATE KEY VALUES  IX2114.2
095200*                    WWWWWWXXXX368ALTKEY1 THROUGH WWWXXXXXXX372   IX2114.2
095300*                    ALTKEY1 RESPECTIVELY).  THE RECORD IN WHICH  IX2114.2
095400*                    THE ALTERNATE KEY VALUE WAS CHANGED TO       IX2114.2
095500*                    WWWWWWXXXX369ALTKEY1 (SEQUENTIAL RECORD 184  IX2114.2
095600*                    AFTER THE REORDING OCCURS I.E, AS A RESULT OFIX2114.2
095700*                    CHANGING THE ALTERNATE KEY VALUE IN          IX2114.2
095800*                    RWRT-TEST-GF-01) SHOULD BE MADE AVAILABLE.   IX2114.2
095900*                                                                 IX2114.2
096000     READ    IX-FD1  NEXT  AT END                                 IX2114.2
096100                     MOVE  "AT END ON READ"  TO COMPUTED-A        IX2114.2
096200                     MOVE   "SUCCESSFUL READ"  TO CORRECT-A       IX2114.2
096300                     GO TO   READ-FAIL-F1-04.                     IX2114.2
096400     MOVE    IX-FD1R1-F-G-240  TO FILE-RECORD-INFO (1).           IX2114.2
096500     IF      EXCUT-COUNTER-06V00  EQUAL TO  1                     IX2114.2
096600             MOVE  182 TO RECKEY-NUM    ELSE                      IX2114.2
096700             ADD     001  TO RECKEY-NUM.                          IX2114.2
096800     IF      XRECORD-NUMBER (1)  NOT EQUAL TO RECKEY-NUM          IX2114.2
096900             MOVE    XRECORD-NUMBER (1)  TO COMPUTED-18V0         IX2114.2
097000             MOVE    RECKEY-NUM TO CORRECT-18V0                   IX2114.2
097100             GO TO   READ-FAIL-F1-04.                             IX2114.2
097200     IF      EXCUT-COUNTER-06V00  EQUAL TO  1                     IX2114.2
097300             MOVE  184  TO RECKEY-NUM.                            IX2114.2
097400     ADD     001  TO EXCUT-COUNTER-06V00.                         IX2114.2
097500     IF      EXCUT-COUNTER-06V00  NOT LESS THAN 4                 IX2114.2
097600             PERFORM  PASS                                        IX2114.2
097700             GO TO READ-WRITE-F1-04.                              IX2114.2
097800     GO TO READ-TEST-F1-04.                                       IX2114.2
097900 READ-FAIL-F1-04.                                                 IX2114.2
098000     PERFORM  FAIL.                                               IX2114.2
098100     MOVE "IX-28, IX-32" TO RE-MARK.                              IX2114.2
098200 READ-WRITE-F1-04.                                                IX2114.2
098300     PERFORM PRINT-DETAIL.                                        IX2114.2
098400     CLOSE   IX-FD1.                                              IX2114.2
098500                                                                  IX2114.2
098600                                                                  IX2114.2
098700 READ-INIT-F1-004.                                                IX2114.2
098800     OPEN    INPUT IX-FD1.                                        IX2114.2
098900     MOVE    "READ UPDATED ALTKEY"  TO FEATURE.                   IX2114.2
099000     MOVE    "READ-TEST-F1-004" TO PAR-NAME.                      IX2114.2
099100     MOVE    "SSSSSSSSSS360"  TO FD1-RECKEY-1-13.                 IX2114.2
099200     MOVE    "WWWWWWWWWW360ALTKEY1"  TO FD1-ALTKEY1-1-20.         IX2114.2
099300     MOVE    "FFFFFFFFFF040ALTKEY2"  TO FD1-ALTKEY2-1-20.         IX2114.2
099400     MOVE    WRK-FD1-RECKEY TO IX-REC-KEY-AREA.                   IX2114.2
099500     MOVE    WRK-FD1-ALTKEY1  TO IX-ALT-KEY1-AREA.                IX2114.2
099600     MOVE    WRK-FD1-ALTKEY2 TO IX-ALT-KEY2-AREA.                 IX2114.2
099700     MOVE    180  TO RECKEY-NUM.                                  IX2114.2
099800     START   IX-FD1                                               IX2114.2
099900                    KEY IS EQUAL TO IX-FD1-ALTKEY1                IX2114.2
100000       INVALID KEY   MOVE  "INVALID KEY ON START"  TO COMPUTED-A  IX2114.2
100100             GO TO READ-FAIL-004.                                 IX2114.2
100200     MOVE    179  TO RECKEY-NUM.                                  IX2114.2
100300     MOVE    ZERO TO EXCUT-COUNTER-06V00.                         IX2114.2
100400     MOVE   4     TO REC-CT.                                      IX2114.2
100500 READ-TEST-F1.                                                    IX2114.2
100600     ADD 1 TO REC-CT.                                             IX2114.2
100700     MOVE "READ  UPDATED RECS  " TO FEATURE.                      IX2114.2
100800     MOVE "READ-TEST-F1-       " TO PAR-NAME.                     IX2114.2
100900*                                                                 IX2114.2
101000*READ-TEST-F1-04 - THIS TEST READS THAT SEGMENT OF THE FILE       IX2114.2
101100*                  UPDATED IN THE FIRST 4 TESTS AND CHECKS THE NEWIX2114.2
101200*                  SEQUENTIAL ORDER OF THE RECORDS. THE SEQUENTIALIX2114.2
101300*                  RETRIEVAL OF THE RECORDS FROM THE FILE IS      IX2114.2
101400*                  EXPECTED TO REFLECT THE UPDATED KEY SEQUENCE.  IX2114.2
101500*                                       THE START STATEMENT       IX2114.2
101600*                  EXTABLISHES ALTERNATE KEY1  AS THE KEY OF REF- IX2114.2
101700*                  ERENCE AND CURRENT RECORD POINTER TO POINT TO  IX2114.2
101800*                  RELATIVE  RECORD NUMBER 180 (ALTERNATE-KEY1    IX2114.2
101900*                  VALUE WWWWWWWWWW360ALTKEY1   BEFORE THE FILE   IX2114.2
102000*                  READING BEGINS.                                IX2114.2
102100*                                                                 IX2114.2
102200     ADD     001  TO RECKEY-NUM.                                  IX2114.2
102300     READ    IX-FD1 NEXT  AT END                                  IX2114.2
102400             MOVE  "AT END ON READ"  TO COMPUTED-A                IX2114.2
102500             GO TO READ-FAIL-004.                                 IX2114.2
102600     MOVE    IX-FD1R1-F-G-240 TO FILE-RECORD-INFO (1).            IX2114.2
102700     IF      RECKEY-NUM EQUAL TO 182                              IX2114.2
102800             MOVE  "YES"  TO ADJUSTED-NUM                         IX2114.2
102900             ADD  001  TO RECKEY-NUM.                             IX2114.2
103000*                                                                 IX2114.2
103100*    THE ABOVE IF STATEMENT ADJUSTS THE RECKEY-NUM TO ACCOUNT     IX2114.2
103200*    FOR THE VOID LEFT IN THE NUMBERING SEQUENCE WHEN THE RECORD  IX2114.2
103300*    WAS REWRITTEN WITH A NEW ALTERNATE KEY.  THE RELATIVE        IX2114.2
103400*    RETREIVAL POSITION OF THE RECORD IN THE FILE SHOULD HAVE     IX2114.2
103500*    CHANGED FROM 182  TO 184.                                    IX2114.2
103600*                                                                 IX2114.2
103700     IF      RECKEY-NUM EQUAL TO 185                              IX2114.2
103800             AND  ADJUSTED-NUM EQUAL TO "YES"                     IX2114.2
103900             MOVE  "NO" TO ADJUSTED-NUM                           IX2114.2
104000             SUBTRACT  001  FROM RECKEY-NUM                       IX2114.2
104100*                                                                 IX2114.2
104200*     THE SUBTRACT STATEMENT IS TO READJUST RECKEY-NUM  FOR       IX2114.2
104300*    INSERTED     RECORD CAUSED BY UPDATE OF ALTERNATE KEY IN     IX2114.2
104400*    THE RECORD.  THE SEQUENTIAL RETRIEVAL POSITION OF UPDATED    IX2114.2
104500*    RECORD SHOULD BE POSITION NUMBER 184.                        IX2114.2
104600*                                                                 IX2114.2
104700             PERFORM  READ-TEST-004-1                             IX2114.2
104800             ADD   001  TO EXCUT-COUNTER-06V00                    IX2114.2
104900             GO TO READ-TEST-F1.                                  IX2114.2
105000     IF      XRECORD-NUMBER (1)  EQUAL TO RECKEY-NUM              IX2114.2
105100             PERFORM  READ-PASS-004                               IX2114.2
105200             ELSE                                                 IX2114.2
105300             MOVE  XRECORD-NUMBER (1)  TO COMPUTED-18V0           IX2114.2
105400             PERFORM  READ-FAIL-004.                              IX2114.2
105500     ADD     001  TO EXCUT-COUNTER-06V00.                         IX2114.2
105600     IF      EXCUT-COUNTER-06V00  LESS THAN 11                    IX2114.2
105700             GO TO READ-TEST-F1.                                  IX2114.2
105800     GO TO   READ-END-004.                                        IX2114.2
105900 READ-TEST-004-1.                                                 IX2114.2
106000     IF      XRECORD-NUMBER (1)  EQUAL TO 182                     IX2114.2
106100             PERFORM  READ-PASS-004                               IX2114.2
106200             ELSE                                                 IX2114.2
106300             MOVE  "WWWWWWXXXX369ALTKEY1"  TO CORRECT-A           IX2114.2
106400             MOVE  ALTERNATE-KEY1 (1) TO WRK-FD1-ALTKEY1          IX2114.2
106500             MOVE  FD1-ALTKEY1-1-20  TO COMPUTED-A                IX2114.2
106600             MOVE  SPACE TO P-OR-F                                IX2114.2
106700           MOVE   "ALTERNATE RECORD KEY1 VALUES; IX-33" TO RE-MARKIX2114.2
106800             PERFORM  PRINT-DETAIL                                IX2114.2
106900             MOVE   XRECORD-NUMBER (1)  TO COMPUTED-18V0          IX2114.2
107000             MOVE  182 TO CORRECT-18V0                            IX2114.2
107100             PERFORM FAIL                                         IX2114.2
107200             PERFORM  PRINT-DETAIL.                               IX2114.2
107300 READ-PASS-004.                                                   IX2114.2
107400     PERFORM PASS                                                 IX2114.2
107500     PERFORM PRINT-DETAIL.                                        IX2114.2
107600 READ-FAIL-004.                                                   IX2114.2
107700     PERFORM FAIL.                                                IX2114.2
107800     MOVE    RECKEY-NUM TO CORRECT-18V0.                          IX2114.2
107900     MOVE    "RECORD NUMBER; IX-28, IX-33 "  TO RE-MARK.          IX2114.2
108000     PERFORM PRINT-DETAIL.                                        IX2114.2
108100 READ-END-004.                                                    IX2114.2
108200     CLOSE   IX-FD1.                                              IX2114.2
108300                                                                  IX2114.2
108400     GO TO CCVS-EXIT.                                             IX2114.2
108500                                                                  IX2114.2
108600                                                                  IX2114.2
108700 INX-INITIALIZE-RECORD.                                           IX2114.2
108800     MOVE    "GGGGGGGGGG200" TO  FD1-RECKEY-1-13.                 IX2114.2
108900     MOVE    ZERO  TO INIT-FLAG.                                  IX2114.2
109000     MOVE    9999  TO XRECORD-NUMBER (1).                         IX2114.2
109100     MOVE    SPACE TO IX-FD1R1-F-G-240.                           IX2114.2
109200     MOVE    WRK-FD1-RECKEY TO IX-REC-KEY-AREA.                   IX2114.2
109300     START   IX-FD1                                               IX2114.2
109400                     KEY IS EQUAL TO IX-FD1-KEY                   IX2114.2
109500          INVALID KEY MOVE   1  TO INIT-FLAG.                     IX2114.2
109600     READ    IX-FD1 NEXT INTO FILE-RECORD-INFO (1)                IX2114.2
109700                     AT END  MOVE  1 TO INIT-FLAG.                IX2114.2
109800     IF      XRECORD-NUMBER (1) NOT EQUAL TO 100                  IX2114.2
109900             MOVE 1 TO INIT-FLAG.                                 IX2114.2
110000 DISPLAY-RECORD-KEYS.                                             IX2114.2
110100     MOVE    XRECORD-KEY (1)  TO WRK-FD1-RECKEY.                  IX2114.2
110200     MOVE    FD1-RECKEY-1-13  TO COMPUTED-A.                      IX2114.2
110300     MOVE    RECKEY-VALUE (RECNO)  TO CORRECT-A.                  IX2114.2
110400     MOVE    SPACE TO P-OR-F.                                     IX2114.2
110500     MOVE    "RECORD KEY VALUES" TO RE-MARK.                      IX2114.2
110600     PERFORM PRINT-DETAIL.                                        IX2114.2
110700 DISPLAY-ALTERNATE-KEY1.                                          IX2114.2
110800     MOVE    ALTERNATE-KEY1 (1)  TO WRK-FD1-ALTKEY1.              IX2114.2
110900     MOVE    FD1-ALTKEY1-1-20  TO COMPUTED-A.                     IX2114.2
111000     MOVE    ALTKEY1-VALUE (RECNO)  TO CORRECT-A.                 IX2114.2
111100     MOVE    SPACE TO P-OR-F.                                     IX2114.2
111200     MOVE    "ALTERNATE RECORD KEY1 VALUES"  TO RE-MARK.          IX2114.2
111300     PERFORM PRINT-DETAIL.                                        IX2114.2
111400 DISPLAY-ALTERNATE-KEY2.                                          IX2114.2
111500     MOVE    ALTERNATE-KEY2 (1) TO WRK-FD1-ALTKEY2.               IX2114.2
111600     MOVE    FD1-ALTKEY2-1-20  TO COMPUTED-A.                     IX2114.2
111700     MOVE    ALTKEY2-VALUE (RECNO)  TO CORRECT-A.                 IX2114.2
111800     MOVE    SPACE TO P-OR-F.                                     IX2114.2
111900     MOVE    "ALTERNATE RECORD KEY2 VALUES"  TO RE-MARK.          IX2114.2
112000     PERFORM PRINT-DETAIL.                                        IX2114.2
112100                                                                  IX2114.2
112200                                                                  IX2114.2
112300 CCVS-EXIT SECTION.                                               IX2114.2
112400 CCVS-999999.                                                     IX2114.2
112500     GO TO CLOSE-FILES.                                           IX2114.2
      *END-OF,IX211A                                                                  
