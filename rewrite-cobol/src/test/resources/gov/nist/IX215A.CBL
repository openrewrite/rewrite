      *HEADER,COBOL,IX215A                                                            
000100 IDENTIFICATION DIVISION.                                         IX2154.2
000200 PROGRAM-ID.                                                      IX2154.2
000300     IX215A.                                                      IX2154.2
000400****************************************************************  IX2154.2
000500*                                                              *  IX2154.2
000600*    VALIDATION FOR:-                                          *  IX2154.2
000700*                                                              *  IX2154.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IX2154.2
000900*                                                              *  IX2154.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".IX2154.2
001100*                                                              *  IX2154.2
001200****************************************************************  IX2154.2
001300*    THE PURPOSE OF THIS PROGRAM IS TO TEST THE ABILITY TO        IX2154.2
001400*    DESCRIBE THE PRIME RECORD KEY AND THE ALTERNATE RECORD KEYS  IX2154.2
001500*    IN A REDEFINES CLAUSES AND TO TEST THE USE OF QUALIFICATION  IX2154.2
001600*    OF THE RECORD KEYS.  THE PROGRAM IS BROKEN INTO THREE SEC-   IX2154.2
001700*    TIONS.  THE FIRST SECTION TESTS THE ABILITY TO USE A         IX2154.2
001800*    REDEFINED DATA ITEM OR A DATA ITEM SUBORDINATE TO IT CON-    IX2154.2
001900*    TAINING THE LEFTMOST CHARACTER POSTIONS OF THE REDEFINED DATAIX2154.2
002000*    ITEM IN THE KEY PHRASE OF THE START STATEMENT.  THE SECOND   IX2154.2
002100*    SECTION TESTS THE ABILITY TO USE A RECORD KEY WHICH IS NESTEDIX2154.2
002200*    IN REDEFINES OR A DATA ITEM SUBORDINATE TO IT THAT CONTAINS  IX2154.2
002300*    THE LEFTMOST CHARACTER POSITIONS OF A REDEFINED DATA ITEM    IX2154.2
002400*    WHICH IS NESTED IN REDEFINES IN THE KEY PHRASE OF THE START  IX2154.2
002500*    STATEMENT.  THE THIRD SECTION TESTS THE USE OF QUALIFICATION IX2154.2
002600*    OF THE RECORD KEYS.  DIFFERENT KEY VALUES ARE USED FOR       IX2154.2
002700*    TESTING.  IF A KEY VALUE IS PROVIDED WHICH MATCHES A RECORD  IX2154.2
002800*    IN THE FILE THE EXECUTION OF A START STATEMENT FOLLOWED BY A IX2154.2
002900*    READ NEXT STATEMENT IS EXPECTED TO MADE AVAILABLE THE RECORD.IX2154.2
003000*    IF A KEY VALUE IS PROVIDED WHICH DOES NOT MATCH ANY RECORD INIX2154.2
003100*    THE FILE THEN THE INVALID KEY PATH IS EXPECTED TO BE TAKEN.  IX2154.2
003200*                                                                 IX2154.2
003300*       REFERENCE    AMERICAN NATIONAL STANDARD                   IX2154.2
003400*                    PROGRAMMING LANGUAGE COBOL, X3.23-1985.      IX2154.2
003500*                    SECTION IX, INDEX I-O, THE START             IX2154.2
003600*                    STATEMENT.  PARAGRAPHS 4.7.3 (2), (3), (4);  IX2154.2
003700*                                           4.7.4 (1), (4), (5),  IX2154.2
003800*                                           (10)   AND            IX2154.2
003900*                    THE REDEFINES CLAUSE PAGE VI-39 5.10.4 (1),  IX2154.2
004000*                                                          (2).   IX2154.2
004100*                                                                 IX2154.2
004200*    BEFORE EACH TEST THE RECORD KEY IS LOAD WITH A KEY VALUE     IX2154.2
004300*    WHICH MAY OR MAY NOT BE A VALID KEY FOR THE FILE.  ALSO      IX2154.2
004400*    BEFORE EACH STEP IN A TEST AN INITIALIZATION PROCEDURE MAY ORIX2154.2
004500*    MAY NOT BE PERFORMED WHICH MAKES AVAILABLE RECORD NUMBER 200.IX2154.2
004600*    IF DURING THIS PROCEDURE AN INVALID KEY OCCURS THE TEST IS   IX2154.2
004700*    DELETED.                                                     IX2154.2
004800*                                                                 IX2154.2
004900*    BEFORE EACH SECTION A INDEXED FILE IS CREATED CONTAINING TWO IX2154.2
005000*    ALTERNATE KEY AND THE ONE REQUIRED RECORD KEY FOR THE FILE.  IX2154.2
005100*    IMMEDIATELY FOLLOWING FILE CREATION THE FILE IS READ AND THE IX2154.2
005200*    RECORDS OF THE FILE VERIFIED FOR ACCURACY.  NEXT THE TESTS   IX2154.2
005300*    ARE EXECUTED USING THE READ, DELETE, REWRITE, and START      IX2154.2
005400*    STATEMENTS.                                                  IX2154.2
005500*                                                                 IX2154.2
005600*    THE RECORDS IN THE FILE ARE CREATED IN SEQUENTIAL ORDER BY   IX2154.2
005700*    RECORD KEY VALUE.  FOLLOWING IS A SAMPLE OF THE DATA CONTENTSIX2154.2
005800*    FOR THE RECORD KEY AND TWO ALTERNATE RECORD KEYS IN THE FILE.IX2154.2
005900*                                                                 IX2154.2
006000*  REC-NO RECORD-KEY     ALTERNATE-KEY-1      ALTERNATE-KEY-2     IX2154.2
006100*  ------ ----------     ---------------      ---------------     IX2154.2
006200*  001   BBBBBBBBBC002  EEEEEEEEEF002ALTKEY1  WWWWWWWWWV398ALTKEY2IX2154.2
006300*  002   BBBBBBBBCC004  EEEEEEEEFF004ALTKEY1  WWWWWWWWVV396ALTKEY2IX2154.2
006400*  003   BBBBBBBCCC006  EEEEEEEFFF006ALTKEY1  WWWWWWWVVV394ALTKEY2IX2154.2
006500*   .          .                  .                     .         IX2154.2
006600*   .          .                  .                     .         IX2154.2
006700*   .          .                  .                     .         IX2154.2
006800*  010   CCCCCCCCCC020  FFFFFFFFFF020ALTKEY1  VVVVVVVVVV380ALTKEY2IX2154.2
006900*  011   CCCCCCCCCD022  FFFFFFFFFG022ALTKEY1  VVVVVVVVVV380ALTKEY2IX2154.2
007000*  012   CCCCCCCCDD024  FFFFFFFFGG024ALTKEY1  VVVVVVVVUU376ALTKEY2IX2154.2
007100*   .          .                  .                     .         IX2154.2
007200*   .          .                  .                     .         IX2154.2
007300*   .          .                  .                     .         IX2154.2
007400*  200   UUUUUUUUUU400  YYYYYYYYYY400ALTKEY1   DDDDDDDDDD000ALTKEYIX2154.2
007500*                                                                 IX2154.2
007600*   NOTE 1 - ALTERNATE-KEY-2 CONTAINS DUPLICATE KEYS EVERY 10TH   IX2154.2
007700*            AND 11TH RECORDS.                                    IX2154.2
007800*                                                                 IX2154.2
007900*   NOTE 2 - THE FIRST 50 RECORDS AND LAST 25 RECORDS OF THE FILE IX2154.2
008000*            FOLLOW THE ABOVE SEQUENTIAL KEY PATTERN.  FOR THE    IX2154.2
008100*            MIDDEL 125 RECORDS ONLY THE NUMBER PART OF THE KEYS  IX2154.2
008200*            ARE VARIED AND VARIED IN THE SEQUENCE SHOWN ABOVE.   IX2154.2
008300*            THAT IS, RECORD KEY AND ALTERNATE-KEY-1 ARE          IX2154.2
008400*            INCREMENTED BY 2 ANDT THE ALTERNATE KEY-2 IS         IX2154.2
008500*            DECREMENTED BY 2 EACH TIME A RECORD IS WRITTEN TO THEIX2154.2
008600*            FILE.  THE FILE IS DESIGNED TO BE LARGE ENOUGH SO    IX2154.2
008700*            THAT AN I-O OPERATION IS REQUIRED FOR EACH RECORD    IX2154.2
008800*            ACCESSED FROM THE FILE.                              IX2154.2
008900*                                                                 IX2154.2
009000*   X-CARD'S WHICH MUST BE REPLACED WITH IMPLEMENTOR-NAMES' IN    IX2154.2
009100*   THIS PROGRAM ARE:                                             IX2154.2
009200*                                                                 IX2154.2
009300*            X-24      IMPLEMENTOR-NAME IN ASSIGN TO CLAUSE FOR   IX2154.2
009400*                        INDEXED FILE-1.                          IX2154.2
009500*            X-25      IMPLEMENTOR-NAME IN ASSIGN TO CLAUSE FOR   IX2154.2
009600*                        INDEXED FILE-2.                          IX2154.2
009700*            X-26      IMPLEMENTOR-NAME IN ASSIGN TO CLAUSE FOR   IX2154.2
009800*                        INDEXED FILE-3.                          IX2154.2
009900*            X-44      SYSTEM-NAME IN ASSIGN TO CLAUSE FOR INDEXEDIX2154.2
010000*                        FILE-1 IF NEEDED.                        IX2154.2
010100*            X-45      SYSTEM-NAME IN ASSIGN TO CLAUSE FOR INDEXEDIX2154.2
010200*                        FILE-2 IF NEEDED.                        IX2154.2
010300*            X-46      SYSTEM-NAME IN ASSIGN TO CLAUSE FOR INDEXEDIX2154.2
010400*                        FILE-3 IF NEEDED.                        IX2154.2
010500*            X-55      SYSTEM PRINTER.                            IX2154.2
010600*            X-62   FOR RAW-DATA                                  IX2154.2
010700*            X-82      SOURCE-COMPUTER.                           IX2154.2
010800*            X-83      OBJECT-COMPUTER.                           IX2154.2
010900*                                                                 IX2154.2
011000******************************************************************IX2154.2
011100*                                                                 IX2154.2
011200 ENVIRONMENT DIVISION.                                            IX2154.2
011300 CONFIGURATION SECTION.                                           IX2154.2
011400 SOURCE-COMPUTER.                                                 IX2154.2
011500     XXXXX082.                                                    IX2154.2
011600 OBJECT-COMPUTER.                                                 IX2154.2
011700     XXXXX083                                                     IX2154.2
011800          PROGRAM COLLATING SEQUENCE IS FOR-INX-START-TEST.       IX2154.2
011900 SPECIAL-NAMES.                                                   IX2154.2
012000     ALPHABET                                                     IX2154.2
012100     FOR-INX-START-TEST IS "WVUTSRJIHGFEDCB".                     IX2154.2
012200 INPUT-OUTPUT SECTION.                                            IX2154.2
012300 FILE-CONTROL.                                                    IX2154.2
012400*    SELECT RAW-DATA   ASSIGN TO                                  IX2154.2
012500*    XXXXX062                                                     IX2154.2
012600*           ORGANIZATION IS INDEXED                               IX2154.2
012700*           ACCESS MODE IS RANDOM                                 IX2154.2
012800*           RECORD KEY IS RAW-DATA-KEY.                           IX2154.2
012900     SELECT PRINT-FILE ASSIGN TO                                  IX2154.2
013000     XXXXX055.                                                    IX2154.2
013100     SELECT  IX-FD1                                               IX2154.2
013200     ASSIGN TO                                                    IX2154.2
013300     XXXXX024                                                     IX2154.2
013400*    XXXXX044                                                     IX2154.2
013500        ACCESS MODE IS DYNAMIC                                    IX2154.2
013600        RECORD KEY IS IX-FD1-KEY                                  IX2154.2
013700        ALTERNATE RECORD KEY IS IX-FD1-ALTKEY1                    IX2154.2
013800        ALTERNATE RECORD KEY IS IX-FD1-ALTKEY2 WITH DUPLICATES    IX2154.2
013900        ORGANIZATION IS INDEXED.                                  IX2154.2
014000     SELECT  IX-FD2                                               IX2154.2
014100     ASSIGN TO                                                    IX2154.2
014200     XXXXX025                                                     IX2154.2
014300*    XXXXX045                                                     IX2154.2
014400        ACCESS MODE IS DYNAMIC                                    IX2154.2
014500        ORGANIZATION IS INDEXED                                   IX2154.2
014600        RECORD KEY IS IX-FD2-KEY                                  IX2154.2
014700        ALTERNATE RECORD KEY IS IX-FD2-ALTKEY1                    IX2154.2
014800        ALTERNATE RECORD KEY IS IX-FD2-ALTKEY2 WITH DUPLICATES    IX2154.2
014900                .                                                 IX2154.2
015000     SELECT  IX-FD3                                               IX2154.2
015100     ASSIGN TO                                                    IX2154.2
015200     XXXXX026                                                     IX2154.2
015300*    XXXXX046                                                     IX2154.2
015400        ACCESS MODE IS DYNAMIC                                    IX2154.2
015500        ORGANIZATION IS INDEXED                                   IX2154.2
015600        RECORD KEY IS IX-FD3-KEY IN IX-FD3-RECKEY-AREA            IX2154.2
015700        ALTERNATE RECORD KEY IS IX-FD3-KEY OF                     IX2154.2
015800         IX-FD3-ALTKEY1-AREA                                      IX2154.2
015900        ALTERNATE RECORD KEY IS IX-FD3-KEY                        IX2154.2
016000         IN IX-FD3-ALTKEY2-AREA                                   IX2154.2
016100        WITH DUPLICATES                                           IX2154.2
016200              .                                                   IX2154.2
016300 DATA DIVISION.                                                   IX2154.2
016400 FILE SECTION.                                                    IX2154.2
016500*                                                                 IX2154.2
016600*FD  RAW-DATA.                                                    IX2154.2
016700*                                                                 IX2154.2
016800*01  RAW-DATA-SATZ.                                               IX2154.2
016900*    05  RAW-DATA-KEY        PIC X(6).                            IX2154.2
017000*    05  C-DATE              PIC 9(6).                            IX2154.2
017100*    05  C-TIME              PIC 9(8).                            IX2154.2
017200*    05  C-NO-OF-TESTS       PIC 99.                              IX2154.2
017300*    05  C-OK                PIC 999.                             IX2154.2
017400*    05  C-ALL               PIC 999.                             IX2154.2
017500*    05  C-FAIL              PIC 999.                             IX2154.2
017600*    05  C-DELETED           PIC 999.                             IX2154.2
017700*    05  C-INSPECT           PIC 999.                             IX2154.2
017800*    05  C-NOTE              PIC X(13).                           IX2154.2
017900*    05  C-INDENT            PIC X.                               IX2154.2
018000*    05  C-ABORT             PIC X(8).                            IX2154.2
018100 FD  PRINT-FILE.                                                  IX2154.2
018200 01  PRINT-REC PICTURE X(120).                                    IX2154.2
018300 01  DUMMY-RECORD PICTURE X(120).                                 IX2154.2
018400 FD  IX-FD1                                                       IX2154.2
018500C    LABEL RECORDS ARE STANDARD                                   IX2154.2
018600C    DATA RECORD IS IX-FD1R1-F-G-240                              IX2154.2
018700     RECORD CONTAINS 240 CHARACTERS.                              IX2154.2
018800 01  IX-FD1R1-F-G-240.                                            IX2154.2
018900     05 IX-FD1-REC-120           PICTURE X(120).                  IX2154.2
019000     05 IX-FD1-REC-121-240.                                       IX2154.2
019100        10 FILLER                PICTURE X(8).                    IX2154.2
019200        10 IX-REC-KEY-AREA.                                       IX2154.2
019300           15 IX-FD1-KEY.                                         IX2154.2
019400              20 IX-FD1-KEY-1-10.                                 IX2154.2
019500                 25 IX-FD1-KEY-1-5      PICTURE X(5).             IX2154.2
019600                 25 IX-FD1-KEY-6-10     PICTURE X(5).             IX2154.2
019700               20 IX-FD1-KEY-11-13      PICTURE X(3).             IX2154.2
019800           15 IX-REDF-RECKEY REDEFINES IX-FD1-KEY.                IX2154.2
019900              20 R-RECKEY-1-7    PICTURE X(7).                    IX2154.2
020000              20 R-REDF-RECKEY-1-7 REDEFINES R-RECKEY-1-7.        IX2154.2
020100                 25 R-RECKEY-1-5        PICTURE X(5).             IX2154.2
020200                 25 R-RECKEY-6-7        PICTURE XX.               IX2154.2
020300              20 R-RECKEY-8-13   PICTURE X(6).                    IX2154.2
020400           15 FILLER             PICTURE X(16).                   IX2154.2
020500        10 FILLER                PICTURE X(9).                    IX2154.2
020600        10 IX-ALT-KEY1-AREA.                                      IX2154.2
020700           15 IX-FD1-ALTKEY1.                                     IX2154.2
020800              20 IX-FDW-ALTKEY1-1-10.                             IX2154.2
020900                 25 IX-FDW-ALTKEY1-1-5  PICTURE X(5).             IX2154.2
021000                 25 IX-FDW-ALTKEY1-6-10 PICTURE X(5).             IX2154.2
021100              20 IX-FDW-ALTKEY1-11-13   PICTURE X(3).             IX2154.2
021200              20 IX-FDW-ALTKEY1-14-20   PICTURE X(7).             IX2154.2
021300           15 IX-REDF-ALTKEY1 REDEFINES IX-FD1-ALTKEY1.           IX2154.2
021400              20 R-ALTKEY1-1-6   PICTURE X(6).                    IX2154.2
021500              20 R-REDF-ALTKEY1-1-6 REDEFINES R-ALTKEY1-1-6.      IX2154.2
021600                 25 R-ALTKEY1-1-4       PICTURE X(4).             IX2154.2
021700                 25 R-ALTKEY1-5-6       PICTURE XX.               IX2154.2
021800              20 R-ALTKEY1-7-10  PICTURE X(4).                    IX2154.2
021900              20 R-ALTKEY1-11-20 PICTURE X(10).                   IX2154.2
022000           15 FILLER             PICTURE X(9).                    IX2154.2
022100        10 FILLER                PICTURE X(9).                    IX2154.2
022200        10 IX-ALT-KEY2-AREA.                                      IX2154.2
022300           15 IX-FD1-ALTKEY2.                                     IX2154.2
022400              20 IX-FDW-ALTKEY2-1-10.                             IX2154.2
022500                 25 IX-FDW-ALTKEY2-1-5  PICTURE X(5).             IX2154.2
022600                 25 IX-FDW-ALTKEY2-6-10 PICTURE X(5).             IX2154.2
022700              20 IX-FDW-ALTKEY2-11-13   PICTURE X(3).             IX2154.2
022800              20 IX-FDW-ALTKEY2-14-20   PICTURE X(7).             IX2154.2
022900           15 IX-REDF-ALTKEY2 REDEFINES IX-FD1-ALTKEY2.           IX2154.2
023000              20 R-ALTKEY2-1-3          PICTURE XXX.              IX2154.2
023100              20 R-REDF-ALTKEY2-1-3 REDEFINES R-ALTKEY2-1-3.      IX2154.2
023200                 25 R-ALTKEY2-1-2       PICTURE XX.               IX2154.2
023300                 25 R-ALTKEY2-3-3       PICTURE X.                IX2154.2
023400              20 R-ALTKEY2-4-20         PICTURE X(17).            IX2154.2
023500           15 FILLER             PICTURE X(9).                    IX2154.2
023600        10 FILLER                PICTURE X(7).                    IX2154.2
023700 FD  IX-FD2                                                       IX2154.2
023800C    LABEL RECORDS ARE STANDARD                                   IX2154.2
023900C    DATA RECORD IS IX-FD2R1-F-G-241                              IX2154.2
024000     BLOCK CONTAINS 4 RECORDS                                     IX2154.2
024100     RECORD CONTAINS 241 CHARACTERS.                              IX2154.2
024200 01  IX-FD2R1-F-G-241.                                            IX2154.2
024300   03 IX-FD2-REC-241.                                             IX2154.2
024400     05 IX-FD2-REC-120           PICTURE X(120).                  IX2154.2
024500     05 IX-FD2-REC-121-241.                                       IX2154.2
024600        10 IX-FD2-RECKEY-AREA    PICTURE X(37).                   IX2154.2
024700        10 IX-FD2-RECKEY-AREA2 REDEFINES IX-FD2-RECKEY-AREA.      IX2154.2
024800           15 IX-FD2-RECKEY-AREA2-1  PICTURE X(23).               IX2154.2
024900           15 IX-FD2-RECKEY-AREA2-2  PICTURE X(14).               IX2154.2
025000        10 IX-FD2-RECKEY-AREA3 REDEFINES IX-FD2-RECKEY-AREA.      IX2154.2
025100           15 FILLER             PICTURE X(8).                    IX2154.2
025200           15 IX-FD2-KEY         PICTURE X(13).                   IX2154.2
025300           15 FILLER             PICTURE X(16).                   IX2154.2
025400        10 IX-FD2-RECKEY-AREA4 REDEFINES IX-FD2-RECKEY-AREA.      IX2154.2
025500           15 FILLER             PICTURE X(8).                    IX2154.2
025600           15 IX-FD2-RECKEY-REDF PICTURE X(13).                   IX2154.2
025700           15 IX-FD2-RECKEY-REDF2 REDEFINES IX-FD2-RECKEY-REDF.   IX2154.2
025800              20 IX-FD2-RECKEY-1-6  PICTURE X(6).                 IX2154.2
025900              20 IX-FD2-RECKEY-7-13  PICTURE X(7).                IX2154.2
026000           15 FILLER             PICTURE X(16).                   IX2154.2
026100        10 FILLER                PICTURE X(9).                    IX2154.2
026200        10 IX-FD2-ALTKEY1-AREA   PICTURE X(29).                   IX2154.2
026300        10 IX-FD2-ALTKEY1-AREA2 REDEFINES IX-FD2-ALTKEY1-AREA.    IX2154.2
026400           15 IX-FD2-ALTKEY1-1-6  PICTURE X(6).                   IX2154.2
026500           15 IX-FD2-ALTKEY1-7-20  PICTURE X(14).                 IX2154.2
026600           15 FILLER             PICTURE X(9).                    IX2154.2
026700        10 IX-FD2-ALTKEY1-AREA3 REDEFINES IX-FD2-ALTKEY1-AREA.    IX2154.2
026800           15 IX-FD2-ALTKEY1.                                     IX2154.2
026900              20 IX-FD2-ALTKEY1-10  PICTURE X(10).                IX2154.2
027000              20 IX-FD2-ALTKEY1-11-20  PICTURE X(10).             IX2154.2
027100           15 IX-FD2-REDF-ALTKEY1 REDEFINES IX-FD2-ALTKEY1.       IX2154.2
027200              20 IX-FD2-ALTKEY1-1-5   PICTURE X(5).               IX2154.2
027300              20 IX-FD2-ALTKEY1-6-20  PICTURE X(15).              IX2154.2
027400           15 FILLER             PICTURE X(9).                    IX2154.2
027500        10 FILLER                PICTURE X(9).                    IX2154.2
027600        10 IX-FD2-ALTKEY2-AREA   PICTURE X(29).                   IX2154.2
027700        10 IX-FD2-ALTKEY2-AREA2 REDEFINES IX-FD2-ALTKEY2-AREA.    IX2154.2
027800           15 IX-FD2-ALTKEY2     PICTURE X(20).                   IX2154.2
027900           15 FILLER             PICTURE X(9).                    IX2154.2
028000        10 IX-FD2-ALTKEY2-AREA3 REDEFINES IX-FD2-ALTKEY2-AREA.    IX2154.2
028100           15 IX-FD2-ALTKEY2-1-6  PICTURE X(6).                   IX2154.2
028200           15 IX-FD2-REDF-ALTKEY2-1-6                             IX2154.2
028300              REDEFINES IX-FD2-ALTKEY2-1-6.                       IX2154.2
028400              20 IX-FD2-ALTKEY2-1-3  PICTURE XXX.                 IX2154.2
028500              20 IX-FD2-ALTKEY2-4-6  PICTURE XXX.                 IX2154.2
028600           15 IX-FD2-ALTKEY2-7-20  PICTURE X(14).                 IX2154.2
028700           15 FILLER             PICTURE X(9).                    IX2154.2
028800        10 FILLER                PICTURE X(8).                    IX2154.2
028900    03  IX-FD2-REC-241-240 REDEFINES IX-FD2-REC-241.              IX2154.2
029000       05 IX-FD2-REC-240   PIC X(240).                            IX2154.2
029100       05 FILLER           PIC X.                                 IX2154.2
029200 FD  IX-FD3                                                       IX2154.2
029300C    LABEL RECORDS ARE STANDARD                                   IX2154.2
029400C    DATA RECORD IS IX-FD3R1-F-G-242                              IX2154.2
029500     RECORD CONTAINS 242 CHARACTERS                               IX2154.2
029600     BLOCK CONTAINS 1694 CHARACTERS.                              IX2154.2
029700 01  IX-FD3R1-F-G-242.                                            IX2154.2
029800   03 IX-FD3-REC-242.                                             IX2154.2
029900     05 IX-FD3-REC-120           PICTURE X(120).                  IX2154.2
030000     05 IX-FD3-REC-121-242.                                       IX2154.2
030100        10 FILLER                PICTURE X(8).                    IX2154.2
030200        10 IX-FD3-RECKEY-AREA.                                    IX2154.2
030300           15 IX-FD3-KEY         PICTURE X(13).                   IX2154.2
030400        10 FILLER                PICTURE X(25).                   IX2154.2
030500        10 IX-FD3-ALTKEY1-AREA.                                   IX2154.2
030600           15 IX-FD3-KEY         PICTURE X(20).                   IX2154.2
030700        10 FILLER                PICTURE X(18).                   IX2154.2
030800        10 IX-FD3-ALTKEY2-AREA.                                   IX2154.2
030900           15 IX-FD3-KEY         PICTURE X(20).                   IX2154.2
031000        10 FILLER                PICTURE X(18).                   IX2154.2
031100     03  IX-FD3-REC-240 REDEFINES IX-FD3-REC-242.                 IX2154.2
031200       05  IX-FD3-240            PICTURE X(240).                  IX2154.2
031300       05  FILLER                PICTURE XX.                      IX2154.2
031400                                                                  IX2154.2
031500                                                                  IX2154.2
031600 WORKING-STORAGE SECTION.                                         IX2154.2
031700 01  WRK-FDW-RECKEY.                                              IX2154.2
031800     05 FDW-RECKEY-1-13.                                          IX2154.2
031900        10 FDW-RECKEY-1-10       PICTURE X(10).                   IX2154.2
032000        10 FDW-RECKEY-11-13      PICTURE 9(3).                    IX2154.2
032100     05 FILLER                   PICTURE X(16)  VALUE SPACE.      IX2154.2
032200 01  WRK-FDW-ALTKEY1.                                             IX2154.2
032300      05 FDW-ALTKEY1-1-20.                                        IX2154.2
032400       10  FDW-ALTKEY1-1-10.                                      IX2154.2
032500        15 FDW-ALTKEY1-1-5       PICTURE X(5).                    IX2154.2
032600        15 FDW-ALTKEY1-6-10      PICTURE X(5).                    IX2154.2
032700       10 FDW-ALTKEY1-11-13      PICTURE 9(3).                    IX2154.2
032800       10 FDW-ALTKEY1-14-20      PICTURE X(7).                    IX2154.2
032900     05 FILLER                   PICTURE X(9)  VALUE SPACE.       IX2154.2
033000 01  WRK-FDW-ALTKEY2.                                             IX2154.2
033100     05 FDW-ALTKEY2-1-20.                                         IX2154.2
033200        10 FDW-ALTKEY2-1-10.                                      IX2154.2
033300           15 FDW-ALTKEY2-1-5    PICTURE X(5).                    IX2154.2
033400           15 FDW-ALTKEY2-6-10   PICTURE X(5).                    IX2154.2
033500        10 FDW-ALTKEY2-11-13     PICTURE 9(3).                    IX2154.2
033600        10 FDW-ALTKEY2-14-20     PICTURE X(7).                    IX2154.2
033700     05 FILLER                   PICTURE X(9) VALUE SPACE.        IX2154.2
033800 01  RECNO                       PICTURE 9(5)  VALUE ZERO.        IX2154.2
033900 01  EXCUT-COUNTER-06V00         PICTURE 9(6) VALUE ZERO.         IX2154.2
034000 01  KEYSUB                      PICTURE 9(3) COMPUTATIONAL.      IX2154.2
034100 01  INVKEY-COUNTER              PICTURE 9(3) COMPUTATIONAL.      IX2154.2
034200 01  RECORDS-WRITTEN             PICTURE 9(3).                    IX2154.2
034300 01  RECKEY-NUM                  PICTURE 9(3).                    IX2154.2
034400 01  ALTKEY1-NUM                 PICTURE 9(3).                    IX2154.2
034500 01  ALTKEY2-NUM                 PICTURE 9(3).                    IX2154.2
034600 01  FAIL-SW                     PICTURE 9 VALUE ZERO.            IX2154.2
034700 01  RECORD-KEY-CONTENT.                                          IX2154.2
034800     05 FILLER PIC X(53) VALUE                                    IX2154.2
034900          "BBBBBBBBBC002EEEEEEEEEF002ALTKEY1WWWWWWWWWV398ALTKEY2".IX2154.2
035000     05 FILLER PIC X(53) VALUE                                    IX2154.2
035100          "BBBBBBBBCC004EEEEEEEEFF004ALTKEY1WWWWWWWWVV396ALTKEY2".IX2154.2
035200     05 FILLER PIC X(53) VALUE                                    IX2154.2
035300          "BBBBBBBCCC006EEEEEEEFFF006ALTKEY1WWWWWWWVVV394ALTKEY2".IX2154.2
035400     05 FILLER PIC X(53) VALUE                                    IX2154.2
035500          "BBBBBBCCCC008EEEEEEFFFF008ALTKEY1WWWWWWVVVV392ALTKEY2".IX2154.2
035600     05 FILLER PIC X(53) VALUE                                    IX2154.2
035700          "BBBBBCCCCC010EEEEEFFFFF010ALTKEY1WWWWWVVVVV390ALTKEY2".IX2154.2
035800     05 FILLER PIC X(53) VALUE                                    IX2154.2
035900          "BBBBCCCCCC012EEEEFFFFFF012ALTKEY1WWWWVVVVVV388ALTKEY2".IX2154.2
036000     05 FILLER PIC X(53) VALUE                                    IX2154.2
036100          "BBBCCCCCCC014EEEFFFFFFF014ALTKEY1WWWVVVVVVV386ALTKEY2".IX2154.2
036200     05 FILLER PIC X(53) VALUE                                    IX2154.2
036300          "BBCCCCCCCC016EEFFFFFFFF016ALTKEY1WWVVVVVVVV384ALTKEY2".IX2154.2
036400     05 FILLER PIC X(53) VALUE                                    IX2154.2
036500          "BCCCCCCCCC018EFFFFFFFFF018ALTKEY1WVVVVVVVVV382ALTKEY2".IX2154.2
036600     05 FILLER PIC X(53) VALUE                                    IX2154.2
036700          "CCCCCCCCCC020FFFFFFFFFF020ALTKEY1VVVVVVVVVV380ALTKEY2".IX2154.2
036800     05 FILLER PIC X(53) VALUE                                    IX2154.2
036900          "CCCCCCCCCD022FFFFFFFFFG022ALTKEY1VVVVVVVVVV380ALTKEY2".IX2154.2
037000     05 FILLER PIC X(53) VALUE                                    IX2154.2
037100          "CCCCCCCCDD024FFFFFFFFGG024ALTKEY1VVVVVVVVUU376ALTKEY2".IX2154.2
037200     05 FILLER PIC X(53) VALUE                                    IX2154.2
037300          "CCCCCCCDDD026FFFFFFFGGG026ALTKEY1VVVVVVVUUU374ALTKEY2".IX2154.2
037400     05 FILLER PIC X(53) VALUE                                    IX2154.2
037500          "CCCCCCDDDD028FFFFFFGGGG028ALTKEY1VVVVVVUUUU372ALTKEY2".IX2154.2
037600     05 FILLER PIC X(53) VALUE                                    IX2154.2
037700          "CCCCCDDDDD030FFFFFGGGGG030ALTKEY1VVVVVUUUUU370ALTKEY2".IX2154.2
037800     05 FILLER PIC X(53) VALUE                                    IX2154.2
037900          "CCCCDDDDDD032FFFFGGGGGG032ALTKEY1VVVVUUUUUU368ALTKEY2".IX2154.2
038000     05 FILLER PIC X(53) VALUE                                    IX2154.2
038100          "CCCDDDDDDD034FFFGGGGGGG034ALTKEY1VVVUUUUUUU366ALTKEY2".IX2154.2
038200     05 FILLER PIC X(53) VALUE                                    IX2154.2
038300          "CCDDDDDDDD036FFGGGGGGGG036ALTKEY1VVUUUUUUUU364ALTKEY2".IX2154.2
038400     05 FILLER PIC X(53) VALUE                                    IX2154.2
038500          "CDDDDDDDDD038FGGGGGGGGG038ALTKEY1VUUUUUUUUU362ALTKEY2".IX2154.2
038600     05 FILLER PIC X(53) VALUE                                    IX2154.2
038700          "DDDDDDDDDD040GGGGGGGGGG040ALTKEY1UUUUUUUUUU360ALTKEY2".IX2154.2
038800     05 FILLER PIC X(53) VALUE                                    IX2154.2
038900          "DDDDDDDDDE042GGGGGGGGGH042ALTKEY1UUUUUUUUUU360ALTKEY2".IX2154.2
039000     05 FILLER PIC X(53) VALUE                                    IX2154.2
039100          "DDDDDDDDEE044GGGGGGGGHH044ALTKEY1UUUUUUUUTT356ALTKEY2".IX2154.2
039200     05 FILLER PIC X(53) VALUE                                    IX2154.2
039300          "DDDDDDDEEE046GGGGGGGHHH046ALTKEY1UUUUUUUTTT354ALTKEY2".IX2154.2
039400     05 FILLER PIC X(53) VALUE                                    IX2154.2
039500          "DDDDDDEEEE048GGGGGGHHHH048ALTKEY1UUUUUUTTTT352ALTKEY2".IX2154.2
039600     05 FILLER PIC X(53) VALUE                                    IX2154.2
039700          "DDDDDEEEEE050GGGGGHHHHH050ALTKEY1UUUUUTTTTT350ALTKEY2".IX2154.2
039800     05 FILLER PIC X(53) VALUE                                    IX2154.2
039900          "DDDDEEEEEE052GGGGHHHHHH052ALTKEY1UUUUTTTTTT348ALTKEY2".IX2154.2
040000     05 FILLER PIC X(53) VALUE                                    IX2154.2
040100          "DDDEEEEEEE054GGGHHHHHHH054ALTKEY1UUUTTTTTTT346ALTKEY2".IX2154.2
040200     05 FILLER PIC X(53) VALUE                                    IX2154.2
040300          "DDEEEEEEEE056GGHHHHHHHH056ALTKEY1UUTTTTTTTT344ALTKEY2".IX2154.2
040400     05 FILLER PIC X(53) VALUE                                    IX2154.2
040500          "DEEEEEEEEE058GHHHHHHHHH058ALTKEY1UTTTTTTTTT342ALTKEY2".IX2154.2
040600     05 FILLER PIC X(53) VALUE                                    IX2154.2
040700          "EEEEEEEEEE060HHHHHHHHHH060ALTKEY1TTTTTTTTTT340ALTKEY2".IX2154.2
040800     05 FILLER PIC X(53) VALUE                                    IX2154.2
040900          "EEEEEEEEEF062HHHHHHHHHI062ALTKEY1TTTTTTTTTT340ALTKEY2".IX2154.2
041000     05 FILLER PIC X(53) VALUE                                    IX2154.2
041100          "EEEEEEEEFF064HHHHHHHHII064ALTKEY1TTTTTTTTSS336ALTKEY2".IX2154.2
041200     05 FILLER PIC X(53) VALUE                                    IX2154.2
041300          "EEEEEEEFFF066HHHHHHHIII066ALTKEY1TTTTTTTSSS334ALTKEY2".IX2154.2
041400     05 FILLER PIC X(53) VALUE                                    IX2154.2
041500          "EEEEEEFFFF068HHHHHHIIII068ALTKEY1TTTTTTSSSS332ALTKEY2".IX2154.2
041600     05 FILLER PIC X(53) VALUE                                    IX2154.2
041700          "EEEEEFFFFF070HHHHHIIIII070ALTKEY1TTTTTSSSSS330ALTKEY2".IX2154.2
041800     05 FILLER PIC X(53) VALUE                                    IX2154.2
041900          "EEEEFFFFFF072HHHHIIIIII072ALTKEY1TTTTSSSSSS328ALTKEY2".IX2154.2
042000     05 FILLER PIC X(53) VALUE                                    IX2154.2
042100          "EEEFFFFFFF074HHHIIIIIII074ALTKEY1TTTSSSSSSS326ALTKEY2".IX2154.2
042200     05 FILLER PIC X(53) VALUE                                    IX2154.2
042300          "EEFFFFFFFF076HHIIIIIIII076ALTKEY1TTSSSSSSSS324ALTKEY2".IX2154.2
042400     05 FILLER PIC X(53) VALUE                                    IX2154.2
042500          "EFFFFFFFFF078HIIIIIIIII078ALTKEY1TSSSSSSSSS322ALTKEY2".IX2154.2
042600     05 FILLER PIC X(53) VALUE                                    IX2154.2
042700          "FFFFFFFFFF080IIIIIIIIII080ALTKEY1SSSSSSSSSS320ALTKEY2".IX2154.2
042800     05 FILLER PIC X(53) VALUE                                    IX2154.2
042900          "FFFFFFFFFG082IIIIIIIIIJ082ALTKEY1SSSSSSSSSS320ALTKEY2".IX2154.2
043000     05 FILLER PIC X(53) VALUE                                    IX2154.2
043100          "FFFFFFFFGG084IIIIIIIIJJ084ALTKEY1SSSSSSSSRR316ALTKEY2".IX2154.2
043200     05 FILLER PIC X(53) VALUE                                    IX2154.2
043300          "FFFFFFFGGG086IIIIIIIJJJ086ALTKEY1SSSSSSSRRR314ALTKEY2".IX2154.2
043400     05 FILLER PIC X(53) VALUE                                    IX2154.2
043500          "FFFFFFGGGG088IIIIIIJJJJ088ALTKEY1SSSSSSRRRR312ALTKEY2".IX2154.2
043600     05 FILLER PIC X(53) VALUE                                    IX2154.2
043700          "FFFFFGGGGG090IIIIIJJJJJ090ALTKEY1SSSSSRRRRR310ALTKEY2".IX2154.2
043800     05 FILLER PIC X(53) VALUE                                    IX2154.2
043900          "FFFFGGGGGG092IIIIJJJJJJ092ALTKEY1SSSSRRRRRR308ALTKEY2".IX2154.2
044000     05 FILLER PIC X(53) VALUE                                    IX2154.2
044100          "FFFGGGGGGG094IIIJJJJJJJ094ALTKEY1SSSRRRRRRR306ALTKEY2".IX2154.2
044200     05 FILLER PIC X(53) VALUE                                    IX2154.2
044300          "FFGGGGGGGG096IIJJJJJJJJ096ALTKEY1SSRRRRRRRR304ALTKEY2".IX2154.2
044400     05 FILLER PIC X(53) VALUE                                    IX2154.2
044500          "FGGGGGGGGG098IJJJJJJJJJ098ALTKEY1SRRRRRRRRR302ALTKEY2".IX2154.2
044600     05 FILLER PIC X(53) VALUE                                    IX2154.2
044700          "GGGGGGGGGG100JJJJJJJJJJ100ALTKEY1RRRRRRRRRR300ALTKEY2".IX2154.2
044800     05 FILLER PIC X(53) VALUE                                    IX2154.2
044900          "RRRRSSSSSS352VVVVWWWWWW352ALTKEY1GGGGFFFFFF048ALTKEY2".IX2154.2
045000     05 FILLER PIC X(53) VALUE                                    IX2154.2
045100          "RRRSSSSSSS354VVVWWWWWWW354ALTKEY1GGGFFFFFFF046ALTKEY2".IX2154.2
045200     05 FILLER PIC X(53) VALUE                                    IX2154.2
045300          "RRSSSSSSSS356VVWWWWWWWW356ALTKEY1GGFFFFFFFF044ALTKEY2".IX2154.2
045400     05 FILLER PIC X(53) VALUE                                    IX2154.2
045500          "RSSSSSSSSS358VWWWWWWWWW358ALTKEY1GFFFFFFFFF042ALTKEY2".IX2154.2
045600     05 FILLER PIC X(53) VALUE                                    IX2154.2
045700          "SSSSSSSSSS360WWWWWWWWWW360ALTKEY1FFFFFFFFFF040ALTKEY2".IX2154.2
045800     05 FILLER PIC X(53) VALUE                                    IX2154.2
045900          "SSSSSSSSST362WWWWWWWWWX362ALTKEY1FFFFFFFFFF040ALTKEY2".IX2154.2
046000     05 FILLER PIC X(53) VALUE                                    IX2154.2
046100          "SSSSSSSSTT364WWWWWWWWXX364ALTKEY1FFFFFFFFEE036ALTKEY2".IX2154.2
046200     05 FILLER PIC X(53) VALUE                                    IX2154.2
046300          "SSSSSSSTTT366WWWWWWWXXX366ALTKEY1FFFFFFFEEE034ALTKEY2".IX2154.2
046400     05 FILLER PIC X(53) VALUE                                    IX2154.2
046500          "SSSSSSTTTT368WWWWWWXXXX368ALTKEY1FFFFFFEEEE032ALTKEY2".IX2154.2
046600     05 FILLER PIC X(53) VALUE                                    IX2154.2
046700          "SSSSSTTTTT370WWWWWXXXXX370ALTKEY1FFFFFEEEEE030ALTKEY2".IX2154.2
046800     05 FILLER PIC X(53) VALUE                                    IX2154.2
046900          "SSSSTTTTTT372WWWWXXXXXX372ALTKEY1FFFFEEEEEE028ALTKEY2".IX2154.2
047000     05 FILLER PIC X(53) VALUE                                    IX2154.2
047100          "SSSTTTTTTT374WWWXXXXXXX374ALTKEY1FFFEEEEEEE026ALTKEY2".IX2154.2
047200     05 FILLER PIC X(53) VALUE                                    IX2154.2
047300          "SSTTTTTTTT376WWXXXXXXXX376ALTKEY1FFEEEEEEEE024ALTKEY2".IX2154.2
047400     05 FILLER PIC X(53) VALUE                                    IX2154.2
047500          "STTTTTTTTT378WXXXXXXXXX378ALTKEY1FEEEEEEEEE022ALTKEY2".IX2154.2
047600     05 FILLER PIC X(53) VALUE                                    IX2154.2
047700          "TTTTTTTTTT380XXXXXXXXXX380ALTKEY1EEEEEEEEEE020ALTKEY2".IX2154.2
047800     05 FILLER PIC X(53) VALUE                                    IX2154.2
047900          "TTTTTTTTTU382XXXXXXXXXY382ALTKEY1EEEEEEEEEE020ALTKEY2".IX2154.2
048000     05 FILLER PIC X(53) VALUE                                    IX2154.2
048100          "TTTTTTTTUU384XXXXXXXXYY384ALTKEY1EEEEEEEEDD016ALTKEY2".IX2154.2
048200     05 FILLER PIC X(53) VALUE                                    IX2154.2
048300          "TTTTTTTUUU386XXXXXXXYYY386ALTKEY1EEEEEEEDDD014ALTKEY2".IX2154.2
048400     05 FILLER PIC X(53) VALUE                                    IX2154.2
048500          "TTTTTTUUUU388XXXXXXYYYY388ALTKEY1EEEEEEDDDD012ALTKEY2".IX2154.2
048600     05 FILLER PIC X(53) VALUE                                    IX2154.2
048700          "TTTTTUUUUU390XXXXXYYYYY390ALTKEY1EEEEEDDDDD010ALTKEY2".IX2154.2
048800     05 FILLER PIC X(53) VALUE                                    IX2154.2
048900          "TTTTUUUUUU392XXXXYYYYYY392ALTKEY1EEEEDDDDDD008ALTKEY2".IX2154.2
049000     05 FILLER PIC X(53) VALUE                                    IX2154.2
049100          "TTTUUUUUUU394XXXYYYYYYY394ALTKEY1EEEDDDDDDD006ALTKEY2".IX2154.2
049200     05 FILLER PIC X(53) VALUE                                    IX2154.2
049300          "TTUUUUUUUU396XXYYYYYYYY396ALTKEY1EEDDDDDDDD004ALTKEY2".IX2154.2
049400     05 FILLER PIC X(53) VALUE                                    IX2154.2
049500          "TUUUUUUUUU398XYYYYYYYYY398ALTKEY1EDDDDDDDDD002ALTKEY2".IX2154.2
049600     05 FILLER PIC X(53) VALUE                                    IX2154.2
049700          "UUUUUUUUUU400YYYYYYYYYY400ALTKEY1DDDDDDDDDD000ALTKEY2".IX2154.2
049800 01  RECORD-KEY-DATA   REDEFINES  RECORD-KEY-CONTENT.             IX2154.2
049900     05 KEY-VALUES                  OCCURS 75  TIMES.             IX2154.2
050000       10 RECKEY-VALUE           PICTURE X(13).                   IX2154.2
050100       10 ALTKEY1-VALUE          PICTURE X(20).                   IX2154.2
050200       10 ALTKEY2-VALUE          PICTURE X(20).                   IX2154.2
050300 01  INIT-FLAG                   PICTURE 9.                       IX2154.2
050400 01  FILE-RECORD-INFORMATION-REC.                                 IX2154.2
050500     03 FILE-RECORD-INFO-SKELETON.                                IX2154.2
050600        05 FILLER                 PICTURE X(48)       VALUE       IX2154.2
050700             "FILE=      ,RECORD=      /0,RECNO=000000,UPDT=00".  IX2154.2
050800        05 FILLER                 PICTURE X(46)       VALUE       IX2154.2
050900             ",ODO=0000,PGM=     ,LRECL=000000,BLKSIZ  =0000".    IX2154.2
051000        05 FILLER                 PICTURE X(26)       VALUE       IX2154.2
051100             ",LFIL=000000,ORG=  ,LBLR= ".                        IX2154.2
051200        05 FILLER                 PICTURE X(37)       VALUE       IX2154.2
051300             ",RECKEY=                             ".             IX2154.2
051400        05 FILLER                 PICTURE X(38)       VALUE       IX2154.2
051500             ",ALTKEY1=                             ".            IX2154.2
051600        05 FILLER                 PICTURE X(38)       VALUE       IX2154.2
051700             ",ALTKEY2=                             ".            IX2154.2
051800        05 FILLER                 PICTURE X(7)        VALUE SPACE.IX2154.2
051900     03 FILE-RECORD-INFO          OCCURS  10  TIMES.              IX2154.2
052000        05 FILE-RECORD-INFO-P1-120.                               IX2154.2
052100           07 FILLER              PIC X(5).                       IX2154.2
052200           07 XFILE-NAME           PIC X(6).                      IX2154.2
052300           07 FILLER              PIC X(8).                       IX2154.2
052400           07 XRECORD-NAME         PIC X(6).                      IX2154.2
052500           07 FILLER              PIC X(1).                       IX2154.2
052600           07 REELUNIT-NUMBER     PIC 9(1).                       IX2154.2
052700           07 FILLER              PIC X(7).                       IX2154.2
052800           07 XRECORD-NUMBER       PIC 9(6).                      IX2154.2
052900           07 FILLER              PIC X(6).                       IX2154.2
053000           07 UPDATE-NUMBER       PIC 9(2).                       IX2154.2
053100           07 FILLER              PIC X(5).                       IX2154.2
053200           07 ODO-NUMBER          PIC 9(4).                       IX2154.2
053300           07 FILLER              PIC X(5).                       IX2154.2
053400           07 XPROGRAM-NAME        PIC X(5).                      IX2154.2
053500           07 FILLER              PIC X(7).                       IX2154.2
053600           07 XRECORD-LENGTH       PIC 9(6).                      IX2154.2
053700           07 FILLER              PIC X(7).                       IX2154.2
053800           07 CHARS-OR-RECORDS    PIC X(2).                       IX2154.2
053900           07 FILLER              PIC X(1).                       IX2154.2
054000           07 XBLOCK-SIZE          PIC 9(4).                      IX2154.2
054100           07 FILLER              PIC X(6).                       IX2154.2
054200           07 RECORDS-IN-FILE     PIC 9(6).                       IX2154.2
054300           07 FILLER              PIC X(5).                       IX2154.2
054400           07 XFILE-ORGANIZATION   PIC X(2).                      IX2154.2
054500           07 FILLER              PIC X(6).                       IX2154.2
054600           07 XLABEL-TYPE          PIC X(1).                      IX2154.2
054700        05 FILE-RECORD-INFO-P121-240.                             IX2154.2
054800           07 FILLER              PIC X(8).                       IX2154.2
054900           07 XRECORD-KEY          PIC X(29).                     IX2154.2
055000           07 FILLER              PIC X(9).                       IX2154.2
055100           07 ALTERNATE-KEY1      PIC X(29).                      IX2154.2
055200           07 FILLER              PIC X(9).                       IX2154.2
055300           07 ALTERNATE-KEY2      PIC X(29).                      IX2154.2
055400           07 FILLER              PIC X(7).                       IX2154.2
055500 01  TEST-RESULTS.                                                IX2154.2
055600     02 FILLER                   PIC X      VALUE SPACE.          IX2154.2
055700     02 FEATURE                  PIC X(20)  VALUE SPACE.          IX2154.2
055800     02 FILLER                   PIC X      VALUE SPACE.          IX2154.2
055900     02 P-OR-F                   PIC X(5)   VALUE SPACE.          IX2154.2
056000     02 FILLER                   PIC X      VALUE SPACE.          IX2154.2
056100     02  PAR-NAME.                                                IX2154.2
056200       03 FILLER                 PIC X(19)  VALUE SPACE.          IX2154.2
056300       03  PARDOT-X              PIC X      VALUE SPACE.          IX2154.2
056400       03 DOTVALUE               PIC 99     VALUE ZERO.           IX2154.2
056500     02 FILLER                   PIC X(8)   VALUE SPACE.          IX2154.2
056600     02 RE-MARK                  PIC X(61).                       IX2154.2
056700 01  TEST-COMPUTED.                                               IX2154.2
056800     02 FILLER                   PIC X(30)  VALUE SPACE.          IX2154.2
056900     02 FILLER                   PIC X(17)  VALUE                 IX2154.2
057000            "       COMPUTED=".                                   IX2154.2
057100     02 COMPUTED-X.                                               IX2154.2
057200     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          IX2154.2
057300     03 COMPUTED-N               REDEFINES COMPUTED-A             IX2154.2
057400                                 PIC -9(9).9(9).                  IX2154.2
057500     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         IX2154.2
057600     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     IX2154.2
057700     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     IX2154.2
057800     03       CM-18V0 REDEFINES COMPUTED-A.                       IX2154.2
057900         04 COMPUTED-18V0                    PIC -9(18).          IX2154.2
058000         04 FILLER                           PIC X.               IX2154.2
058100     03 FILLER PIC X(50) VALUE SPACE.                             IX2154.2
058200 01  TEST-CORRECT.                                                IX2154.2
058300     02 FILLER PIC X(30) VALUE SPACE.                             IX2154.2
058400     02 FILLER PIC X(17) VALUE "       CORRECT =".                IX2154.2
058500     02 CORRECT-X.                                                IX2154.2
058600     03 CORRECT-A                  PIC X(20) VALUE SPACE.         IX2154.2
058700     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      IX2154.2
058800     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         IX2154.2
058900     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     IX2154.2
059000     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     IX2154.2
059100     03      CR-18V0 REDEFINES CORRECT-A.                         IX2154.2
059200         04 CORRECT-18V0                     PIC -9(18).          IX2154.2
059300         04 FILLER                           PIC X.               IX2154.2
059400     03 FILLER PIC X(2) VALUE SPACE.                              IX2154.2
059500     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     IX2154.2
059600 01  CCVS-C-1.                                                    IX2154.2
059700     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PAIX2154.2
059800-    "SS  PARAGRAPH-NAME                                          IX2154.2
059900-    "       REMARKS".                                            IX2154.2
060000     02 FILLER                     PIC X(20)    VALUE SPACE.      IX2154.2
060100 01  CCVS-C-2.                                                    IX2154.2
060200     02 FILLER                     PIC X        VALUE SPACE.      IX2154.2
060300     02 FILLER                     PIC X(6)     VALUE "TESTED".   IX2154.2
060400     02 FILLER                     PIC X(15)    VALUE SPACE.      IX2154.2
060500     02 FILLER                     PIC X(4)     VALUE "FAIL".     IX2154.2
060600     02 FILLER                     PIC X(94)    VALUE SPACE.      IX2154.2
060700 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       IX2154.2
060800 01  REC-CT                        PIC 99       VALUE ZERO.       IX2154.2
060900 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       IX2154.2
061000 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       IX2154.2
061100 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       IX2154.2
061200 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       IX2154.2
061300 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       IX2154.2
061400 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       IX2154.2
061500 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      IX2154.2
061600 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       IX2154.2
061700 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     IX2154.2
061800 01  CCVS-H-1.                                                    IX2154.2
061900     02  FILLER                    PIC X(39)    VALUE SPACES.     IX2154.2
062000     02  FILLER                    PIC X(42)    VALUE             IX2154.2
062100     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 IX2154.2
062200     02  FILLER                    PIC X(39)    VALUE SPACES.     IX2154.2
062300 01  CCVS-H-2A.                                                   IX2154.2
062400   02  FILLER                        PIC X(40)  VALUE SPACE.      IX2154.2
062500   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  IX2154.2
062600   02  FILLER                        PIC XXXX   VALUE             IX2154.2
062700     "4.2 ".                                                      IX2154.2
062800   02  FILLER                        PIC X(28)  VALUE             IX2154.2
062900            " COPY - NOT FOR DISTRIBUTION".                       IX2154.2
063000   02  FILLER                        PIC X(41)  VALUE SPACE.      IX2154.2
063100                                                                  IX2154.2
063200 01  CCVS-H-2B.                                                   IX2154.2
063300   02  FILLER                        PIC X(15)  VALUE             IX2154.2
063400            "TEST RESULT OF ".                                    IX2154.2
063500   02  TEST-ID                       PIC X(9).                    IX2154.2
063600   02  FILLER                        PIC X(4)   VALUE             IX2154.2
063700            " IN ".                                               IX2154.2
063800   02  FILLER                        PIC X(12)  VALUE             IX2154.2
063900     " HIGH       ".                                              IX2154.2
064000   02  FILLER                        PIC X(22)  VALUE             IX2154.2
064100            " LEVEL VALIDATION FOR ".                             IX2154.2
064200   02  FILLER                        PIC X(58)  VALUE             IX2154.2
064300     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IX2154.2
064400 01  CCVS-H-3.                                                    IX2154.2
064500     02  FILLER                      PIC X(34)  VALUE             IX2154.2
064600            " FOR OFFICIAL USE ONLY    ".                         IX2154.2
064700     02  FILLER                      PIC X(58)  VALUE             IX2154.2
064800     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".IX2154.2
064900     02  FILLER                      PIC X(28)  VALUE             IX2154.2
065000            "  COPYRIGHT   1985 ".                                IX2154.2
065100 01  CCVS-E-1.                                                    IX2154.2
065200     02 FILLER                       PIC X(52)  VALUE SPACE.      IX2154.2
065300     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              IX2154.2
065400     02 ID-AGAIN                     PIC X(9).                    IX2154.2
065500     02 FILLER                       PIC X(45)  VALUE SPACES.     IX2154.2
065600 01  CCVS-E-2.                                                    IX2154.2
065700     02  FILLER                      PIC X(31)  VALUE SPACE.      IX2154.2
065800     02  FILLER                      PIC X(21)  VALUE SPACE.      IX2154.2
065900     02 CCVS-E-2-2.                                               IX2154.2
066000         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      IX2154.2
066100         03 FILLER                   PIC X      VALUE SPACE.      IX2154.2
066200         03 ENDER-DESC               PIC X(44)  VALUE             IX2154.2
066300            "ERRORS ENCOUNTERED".                                 IX2154.2
066400 01  CCVS-E-3.                                                    IX2154.2
066500     02  FILLER                      PIC X(22)  VALUE             IX2154.2
066600            " FOR OFFICIAL USE ONLY".                             IX2154.2
066700     02  FILLER                      PIC X(12)  VALUE SPACE.      IX2154.2
066800     02  FILLER                      PIC X(58)  VALUE             IX2154.2
066900     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IX2154.2
067000     02  FILLER                      PIC X(13)  VALUE SPACE.      IX2154.2
067100     02 FILLER                       PIC X(15)  VALUE             IX2154.2
067200             " COPYRIGHT 1985".                                   IX2154.2
067300 01  CCVS-E-4.                                                    IX2154.2
067400     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      IX2154.2
067500     02 FILLER                       PIC X(4)   VALUE " OF ".     IX2154.2
067600     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      IX2154.2
067700     02 FILLER                       PIC X(40)  VALUE             IX2154.2
067800      "  TESTS WERE EXECUTED SUCCESSFULLY".                       IX2154.2
067900 01  XXINFO.                                                      IX2154.2
068000     02 FILLER                       PIC X(19)  VALUE             IX2154.2
068100            "*** INFORMATION ***".                                IX2154.2
068200     02 INFO-TEXT.                                                IX2154.2
068300       04 FILLER                     PIC X(8)   VALUE SPACE.      IX2154.2
068400       04 XXCOMPUTED                 PIC X(20).                   IX2154.2
068500       04 FILLER                     PIC X(5)   VALUE SPACE.      IX2154.2
068600       04 XXCORRECT                  PIC X(20).                   IX2154.2
068700     02 INF-ANSI-REFERENCE           PIC X(48).                   IX2154.2
068800 01  HYPHEN-LINE.                                                 IX2154.2
068900     02 FILLER  PIC IS X VALUE IS SPACE.                          IX2154.2
069000     02 FILLER  PIC IS X(65)    VALUE IS "************************IX2154.2
069100-    "*****************************************".                 IX2154.2
069200     02 FILLER  PIC IS X(54)    VALUE IS "************************IX2154.2
069300-    "******************************".                            IX2154.2
069400 01  CCVS-PGM-ID                     PIC X(9)   VALUE             IX2154.2
069500     "IX215A".                                                    IX2154.2
069600 PROCEDURE DIVISION.                                              IX2154.2
069700 CCVS1 SECTION.                                                   IX2154.2
069800 OPEN-FILES.                                                      IX2154.2
069900*    OPEN I-O RAW-DATA.                                           IX2154.2
070000*    MOVE CCVS-PGM-ID TO RAW-DATA-KEY.                            IX2154.2
070100*    READ RAW-DATA INVALID KEY GO TO END-E-1.                     IX2154.2
070200*    MOVE "ABORTED " TO C-ABORT.                                  IX2154.2
070300*    ADD 1 TO C-NO-OF-TESTS.                                      IX2154.2
070400*    ACCEPT C-DATE  FROM DATE.                                    IX2154.2
070500*    ACCEPT C-TIME  FROM TIME.                                    IX2154.2
070600*    REWRITE RAW-DATA-SATZ INVALID KEY GO TO END-E-1.             IX2154.2
070700*END-E-1.                                                         IX2154.2
070800*    CLOSE RAW-DATA.                                              IX2154.2
070900     OPEN    OUTPUT PRINT-FILE.                                   IX2154.2
071000     MOVE  CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.  IX2154.2
071100     MOVE    SPACE TO TEST-RESULTS.                               IX2154.2
071200     PERFORM HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.              IX2154.2
071300     MOVE    ZERO TO REC-SKL-SUB.                                 IX2154.2
071400     PERFORM CCVS-INIT-FILE 9 TIMES.                              IX2154.2
071500 CCVS-INIT-FILE.                                                  IX2154.2
071600     ADD     1 TO REC-SKL-SUB.                                    IX2154.2
071700     MOVE    FILE-RECORD-INFO-SKELETON                            IX2154.2
071800          TO FILE-RECORD-INFO (REC-SKL-SUB).                      IX2154.2
071900 CCVS-INIT-EXIT.                                                  IX2154.2
072000     GO TO CCVS1-EXIT.                                            IX2154.2
072100 CLOSE-FILES.                                                     IX2154.2
072200*    OPEN I-O RAW-DATA.                                           IX2154.2
072300*    MOVE CCVS-PGM-ID TO RAW-DATA-KEY.                            IX2154.2
072400*    READ RAW-DATA INVALID KEY GO TO END-E-2.                     IX2154.2
072500*    MOVE "OK.     " TO C-ABORT.                                  IX2154.2
072600*    MOVE PASS-COUNTER TO C-OK.                                   IX2154.2
072700*    MOVE ERROR-HOLD   TO C-ALL.                                  IX2154.2
072800*    MOVE ERROR-COUNTER TO C-FAIL.                                IX2154.2
072900*    MOVE DELETE-COUNTER TO C-DELETED.                            IX2154.2
073000*    MOVE INSPECT-COUNTER TO C-INSPECT.                           IX2154.2
073100*    REWRITE RAW-DATA-SATZ INVALID KEY GO TO END-E-2.             IX2154.2
073200*END-E-2.                                                         IX2154.2
073300*    CLOSE RAW-DATA.                                              IX2154.2
073400     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   IX2154.2
073500 TERMINATE-CCVS.                                                  IX2154.2
073600*    EXIT PROGRAM.                                                IX2154.2
073700*TERMINATE-CALL.                                                  IX2154.2
073800     STOP     RUN.                                                IX2154.2
073900 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         IX2154.2
074000 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           IX2154.2
074100 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          IX2154.2
074200 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      IX2154.2
074300     MOVE "****TEST DELETED****" TO RE-MARK.                      IX2154.2
074400 PRINT-DETAIL.                                                    IX2154.2
074500     IF REC-CT NOT EQUAL TO ZERO                                  IX2154.2
074600             MOVE "." TO PARDOT-X                                 IX2154.2
074700             MOVE REC-CT TO DOTVALUE.                             IX2154.2
074800     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      IX2154.2
074900     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               IX2154.2
075000        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 IX2154.2
075100          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 IX2154.2
075200     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              IX2154.2
075300     MOVE SPACE TO CORRECT-X.                                     IX2154.2
075400     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         IX2154.2
075500     MOVE     SPACE TO RE-MARK.                                   IX2154.2
075600 HEAD-ROUTINE.                                                    IX2154.2
075700     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  IX2154.2
075800     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  IX2154.2
075900     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  IX2154.2
076000     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  IX2154.2
076100 COLUMN-NAMES-ROUTINE.                                            IX2154.2
076200     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IX2154.2
076300     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IX2154.2
076400     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        IX2154.2
076500 END-ROUTINE.                                                     IX2154.2
076600     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.IX2154.2
076700 END-RTN-EXIT.                                                    IX2154.2
076800     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IX2154.2
076900 END-ROUTINE-1.                                                   IX2154.2
077000      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      IX2154.2
077100      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               IX2154.2
077200      ADD PASS-COUNTER TO ERROR-HOLD.                             IX2154.2
077300*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   IX2154.2
077400      MOVE PASS-COUNTER TO CCVS-E-4-1.                            IX2154.2
077500      MOVE ERROR-HOLD TO CCVS-E-4-2.                              IX2154.2
077600      MOVE CCVS-E-4 TO CCVS-E-2-2.                                IX2154.2
077700      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           IX2154.2
077800  END-ROUTINE-12.                                                 IX2154.2
077900      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        IX2154.2
078000     IF       ERROR-COUNTER IS EQUAL TO ZERO                      IX2154.2
078100         MOVE "NO " TO ERROR-TOTAL                                IX2154.2
078200         ELSE                                                     IX2154.2
078300         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       IX2154.2
078400     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           IX2154.2
078500     PERFORM WRITE-LINE.                                          IX2154.2
078600 END-ROUTINE-13.                                                  IX2154.2
078700     IF DELETE-COUNTER IS EQUAL TO ZERO                           IX2154.2
078800         MOVE "NO " TO ERROR-TOTAL  ELSE                          IX2154.2
078900         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      IX2154.2
079000     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   IX2154.2
079100     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IX2154.2
079200      IF   INSPECT-COUNTER EQUAL TO ZERO                          IX2154.2
079300          MOVE "NO " TO ERROR-TOTAL                               IX2154.2
079400      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   IX2154.2
079500      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            IX2154.2
079600      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          IX2154.2
079700     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IX2154.2
079800 WRITE-LINE.                                                      IX2154.2
079900     ADD 1 TO RECORD-COUNT.                                       IX2154.2
080000*    IF RECORD-COUNT GREATER 42                                   IX2154.2
080100*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          IX2154.2
080200*        MOVE SPACE TO DUMMY-RECORD                               IX2154.2
080300*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  IX2154.2
080400*        MOVE CCVS-H-1  TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES    IX2154.2
080500*        MOVE CCVS-H-2A TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES    IX2154.2
080600*        MOVE CCVS-H-2B TO DUMMY-RECORD PERFORM WRT-LN 3 TIMES    IX2154.2
080700*        MOVE CCVS-H-3  TO DUMMY-RECORD PERFORM WRT-LN 3 TIMES    IX2154.2
080800*        MOVE CCVS-C-1  TO DUMMY-RECORD PERFORM WRT-LN            IX2154.2
080900*        MOVE CCVS-C-2  TO DUMMY-RECORD PERFORM WRT-LN            IX2154.2
081000*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          IX2154.2
081100*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          IX2154.2
081200*        MOVE ZERO TO RECORD-COUNT.                               IX2154.2
081300     PERFORM WRT-LN.                                              IX2154.2
081400 WRT-LN.                                                          IX2154.2
081500     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               IX2154.2
081600     MOVE SPACE TO DUMMY-RECORD.                                  IX2154.2
081700 BLANK-LINE-PRINT.                                                IX2154.2
081800     PERFORM WRT-LN.                                              IX2154.2
081900 FAIL-ROUTINE.                                                    IX2154.2
082000     IF     COMPUTED-X NOT EQUAL TO SPACE                         IX2154.2
082100            GO TO   FAIL-ROUTINE-WRITE.                           IX2154.2
082200     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.IX2154.2
082300     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 IX2154.2
082400     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   IX2154.2
082500     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IX2154.2
082600     MOVE   SPACES TO INF-ANSI-REFERENCE.                         IX2154.2
082700     GO TO  FAIL-ROUTINE-EX.                                      IX2154.2
082800 FAIL-ROUTINE-WRITE.                                              IX2154.2
082900     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         IX2154.2
083000     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 IX2154.2
083100     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. IX2154.2
083200     MOVE   SPACES TO COR-ANSI-REFERENCE.                         IX2154.2
083300 FAIL-ROUTINE-EX. EXIT.                                           IX2154.2
083400 BAIL-OUT.                                                        IX2154.2
083500     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   IX2154.2
083600     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           IX2154.2
083700 BAIL-OUT-WRITE.                                                  IX2154.2
083800     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  IX2154.2
083900     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 IX2154.2
084000     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IX2154.2
084100     MOVE   SPACES TO INF-ANSI-REFERENCE.                         IX2154.2
084200 BAIL-OUT-EX. EXIT.                                               IX2154.2
084300 CCVS1-EXIT.                                                      IX2154.2
084400     EXIT.                                                        IX2154.2
084500 SECT-0001-IX215A SECTION.                                        IX2154.2
084600 WRITE-INT-GF-01.                                                 IX2154.2
084700     OPEN    OUTPUT IX-FD1.                                       IX2154.2
084800     MOVE    "IX-FD1" TO XFILE-NAME           (1).                IX2154.2
084900     MOVE    "R1-F-G" TO  XRECORD-NAME        (1).                IX2154.2
085000     MOVE    ZERO     TO  XRECORD-NUMBER      (1).                IX2154.2
085100     MOVE    "RC"     TO  CHARS-OR-RECORDS    (1).                IX2154.2
085200     MOVE "IX215"     TO  XPROGRAM-NAME       (1).                IX2154.2
085300     MOVE    240      TO  XRECORD-LENGTH      (1).                IX2154.2
085400     MOVE    001      TO  XBLOCK-SIZE         (1).                IX2154.2
085500     MOVE    "IX"     TO  XFILE-ORGANIZATION  (1).                IX2154.2
085600     MOVE    "S"      TO  XLABEL-TYPE         (1).                IX2154.2
085700     MOVE    200      TO  RECORDS-IN-FILE     (1).                IX2154.2
085800     MOVE    "CREATE-FILE-FD1" TO FEATURE.                        IX2154.2
085900     MOVE    "WRITE-TEST-GF-01" TO PAR-NAME.                      IX2154.2
086000     MOVE    ZERO TO KEYSUB.                                      IX2154.2
086100     MOVE    ZERO TO INVKEY-COUNTER.                              IX2154.2
086200 WRITE-INIT-GF-01-01.                                             IX2154.2
086300     PERFORM WRITE-TEST-GF-01-R1 50 TIMES.                        IX2154.2
086400     PERFORM WRITE-TEST-GF-01-R2 125 TIMES.                       IX2154.2
086500     PERFORM WRITE-TEST-GF-01-R1 25 TIMES.                        IX2154.2
086600     GO TO   WRITE-TEST-GF-01.                                    IX2154.2
086700 WRITE-TEST-GF-01-R1.                                             IX2154.2
086800     ADD     001  TO XRECORD-NUMBER (1).                          IX2154.2
086900     ADD     001  TO  KEYSUB.                                     IX2154.2
087000     MOVE    RECKEY-VALUE (KEYSUB) TO FDW-RECKEY-1-13.            IX2154.2
087100     MOVE    ALTKEY1-VALUE (KEYSUB) TO FDW-ALTKEY1-1-20.          IX2154.2
087200     MOVE    ALTKEY2-VALUE (KEYSUB) TO FDW-ALTKEY2-1-20.          IX2154.2
087300     MOVE    WRK-FDW-RECKEY   TO    XRECORD-KEY (1).              IX2154.2
087400     MOVE    WRK-FDW-ALTKEY1  TO ALTERNATE-KEY1 (1).              IX2154.2
087500     MOVE    WRK-FDW-ALTKEY2  TO ALTERNATE-KEY2 (1).              IX2154.2
087600     MOVE    FILE-RECORD-INFO (1)  TO IX-FD1R1-F-G-240.           IX2154.2
087700     WRITE   IX-FD1R1-F-G-240                                     IX2154.2
087800                        INVALID KEY ADD 001 TO INVKEY-COUNTER.    IX2154.2
087900     ADD     001 TO  EXCUT-COUNTER-06V00.                         IX2154.2
088000 WRITE-TEST-GF-01-R2.                                             IX2154.2
088100     ADD     002  TO FDW-RECKEY-11-13.                            IX2154.2
088200     ADD     002 TO FDW-ALTKEY1-11-13.                            IX2154.2
088300     SUBTRACT  002 FROM FDW-ALTKEY2-11-13.                        IX2154.2
088400     ADD     001  TO XRECORD-NUMBER (1).                          IX2154.2
088500     MOVE    WRK-FDW-RECKEY   TO    XRECORD-KEY (1).              IX2154.2
088600     MOVE    WRK-FDW-ALTKEY1  TO ALTERNATE-KEY1 (1).              IX2154.2
088700     MOVE    WRK-FDW-ALTKEY2  TO ALTERNATE-KEY2 (1).              IX2154.2
088800     MOVE    FILE-RECORD-INFO (1) TO IX-FD1R1-F-G-240.            IX2154.2
088900     WRITE   IX-FD1R1-F-G-240                                     IX2154.2
089000                        INVALID KEY ADD 001 TO INVKEY-COUNTER.    IX2154.2
089100     ADD     001 TO EXCUT-COUNTER-06V00.                          IX2154.2
089200 WRITE-TEST-GF-01.                                                IX2154.2
089300     SUBTRACT INVKEY-COUNTER FROM EXCUT-COUNTER-06V00             IX2154.2
089400             GIVING RECORDS-WRITTEN.                              IX2154.2
089500     IF      RECORDS-WRITTEN EQUAL TO 200                         IX2154.2
089600             PERFORM  PASS                                        IX2154.2
089700             MOVE "FILE IX-FD1 CREATED (200 RECORDS)" TO RE-MARK  IX2154.2
089800     ELSE    PERFORM FAIL                                         IX2154.2
089900             MOVE                                                 IX2154.2
090000       "IX-41;WRONG NUMBER OF RECORDS WRITTEN (MAY ALREADY EXIST)"IX2154.2
090100            TO  RE-MARK                                           IX2154.2
090200             MOVE RECORDS-WRITTEN TO COMPUTED-18V0.               IX2154.2
090300     GO TO   WRITE-TEST-GF-01-END.                                IX2154.2
090400 WRITE-DELETE-GF-01.                                              IX2154.2
090500     PERFORM DE-LETE.                                             IX2154.2
090600 WRITE-TEST-GF-01-END.                                            IX2154.2
090700     PERFORM  PRINT-DETAIL.                                       IX2154.2
090800     CLOSE   IX-FD1.                                              IX2154.2
090900 READ-INIT-F1-01.                                                 IX2154.2
091000     OPEN    INPUT  IX-FD1.                                       IX2154.2
091100     MOVE    "READ-TEST-F1-01" TO PAR-NAME.                       IX2154.2
091200     MOVE    "READ FILE IX-FD1" TO FEATURE.                       IX2154.2
091300     MOVE    ZERO TO EXCUT-COUNTER-06V00.                         IX2154.2
091400     MOVE    02   TO RECKEY-NUM.                                  IX2154.2
091500     MOVE    002 TO ALTKEY1-NUM.                                  IX2154.2
091600 READ-TEST-F1-01-R1.                                              IX2154.2
091700     READ    IX-FD1 NEXT RECORD AT END  GO TO READ-TEST-F1-01.    IX2154.2
091800     MOVE    IX-REC-KEY-AREA TO WRK-FDW-RECKEY.                   IX2154.2
091900     MOVE    IX-ALT-KEY1-AREA TO WRK-FDW-ALTKEY1.                 IX2154.2
092000     IF      FDW-RECKEY-11-13 EQUAL TO RECKEY-NUM                 IX2154.2
092100             AND FDW-ALTKEY1-11-13 EQUAL TO ALTKEY1-NUM           IX2154.2
092200             NEXT SENTENCE                                        IX2154.2
092300             ELSE                                                 IX2154.2
092400             PERFORM  READ-FAIL-F1-01.                            IX2154.2
092500     IF      EXCUT-COUNTER-06V00 NOT LESS THAN 200                IX2154.2
092600             GO TO READ-TEST-F1-01.                               IX2154.2
092700     ADD     001  TO EXCUT-COUNTER-06V00.                         IX2154.2
092800     ADD     002  TO RECKEY-NUM                                   IX2154.2
092900     ADD     002  TO ALTKEY1-NUM.                                 IX2154.2
093000     GO TO   READ-TEST-F1-01-R1.                                  IX2154.2
093100 READ-TEST-F1-01.                                                 IX2154.2
093200     IF      FAIL-SW EQUAL TO 1 GO TO READ-EXIT-F1-01.            IX2154.2
093300     IF      EXCUT-COUNTER-06V00 EQUAL TO 200                     IX2154.2
093400             PERFORM PASS                                         IX2154.2
093500             MOVE "200 RECORDS VERIFIED" TO RE-MARK               IX2154.2
093600     ELSE    PERFORM FAIL                                         IX2154.2
093700     MOVE "INCORRECT NUMBER OF RECORDS; IX-41 OR IX-28" TO RE-MARKIX2154.2
093800             MOVE 200 TO CORRECT-18V0                             IX2154.2
093900             MOVE EXCUT-COUNTER-06V00 TO COMPUTED-18V0.           IX2154.2
094000     PERFORM PRINT-DETAIL.                                        IX2154.2
094100     GO TO   READ-EXIT-F1-01.                                     IX2154.2
094200 READ-FAIL-F1-01.                                                 IX2154.2
094300     MOVE 1 TO FAIL-SW.                                           IX2154.2
094400     PERFORM FAIL.                                                IX2154.2
094500     MOVE    FDW-RECKEY-11-13  TO COMPUTED-18V0.                  IX2154.2
094600     MOVE    RECKEY-NUM  TO CORRECT-18V0.                         IX2154.2
094700     MOVE "READ-FAIL-F1-01; IX-41 OR IX-28" TO RE-MARK.           IX2154.2
094800 READ-EXIT-F1-01.                                                 IX2154.2
094900     CLOSE   IX-FD1.                                              IX2154.2
095000 START-INIT.                                                      IX2154.2
095100     OPEN    I-O  IX-FD1.                                         IX2154.2
095200     MOVE "----------" TO DUMMY-RECORD. PERFORM BLANK-LINE-PRINT. IX2154.2
095300     MOVE    "START-TEST-GF-01" TO PAR-NAME.                      IX2154.2
095400     MOVE    "START REDF REC-KEY" TO FEATURE.                     IX2154.2
095500     MOVE    "BBBBBBBBBC002" TO FDW-RECKEY-1-13.                  IX2154.2
095600     MOVE    WRK-FDW-RECKEY TO IX-REC-KEY-AREA.                   IX2154.2
095700 START-TEST-GF-01.                                                IX2154.2
095800     DELETE  IX-FD1 INVALID KEY                                   IX2154.2
095900             MOVE  "INVALID KEY (DELETE)" TO COMPUTED-A           IX2154.2
096000             GO TO START-FAIL-GF-01.                              IX2154.2
096100     PERFORM START-INIT-FD1 THRU START-INIT-FD1-EXIT.             IX2154.2
096200     IF      INIT-FLAG NOT EQUAL ZERO                             IX2154.2
096300             GO TO START-DELETE-GF-01.                            IX2154.2
096400     MOVE    "BBBBBBBBBC002" TO FDW-RECKEY-1-13.                  IX2154.2
096500     MOVE    WRK-FDW-RECKEY TO IX-REC-KEY-AREA.                   IX2154.2
096600     START   IX-FD1                                               IX2154.2
096700                    KEY IS EQUAL TO IX-REDF-RECKEY                IX2154.2
096800          INVALID KEY    PERFORM PASS                             IX2154.2
096900             MOVE "OK.; INVALID KEY ON START OF DELETED RECORD"   IX2154.2
097000                  TO RE-MARK                                      IX2154.2
097100             GO TO START-WRITE-GF-01.                             IX2154.2
097200     READ    IX-FD1 NEXT RECORD  AT END                           IX2154.2
097300             MOVE "AT END ON READ" TO COMPUTED-A                  IX2154.2
097400             GO TO START-FAIL-GF-01.                              IX2154.2
097500     MOVE    IX-FD1R1-F-G-240  TO  FILE-RECORD-INFO (1).          IX2154.2
097600     MOVE     XRECORD-NUMBER (1)  TO COMPUTED-18V0.               IX2154.2
097700 START-FAIL-GF-01.                                                IX2154.2
097800     PERFORM FAIL.                                                IX2154.2
097900     MOVE    "INVALID KEY ON START" TO CORRECT-A.                 IX2154.2
098000     MOVE "WRONG RECORD NUMBER FOUND; IX-28 OR IX-36" TO RE-MARK. IX2154.2
098100     GO TO   START-WRITE-GF-01.                                   IX2154.2
098200 START-DELETE-GF-01.                                              IX2154.2
098300     PERFORM DE-LETE.                                             IX2154.2
098400 START-WRITE-GF-01.                                               IX2154.2
098500     PERFORM PRINT-DETAIL.                                        IX2154.2
098600 START-INIT-GF-02.                                                IX2154.2
098700     MOVE "----------" TO DUMMY-RECORD. PERFORM BLANK-LINE-PRINT. IX2154.2
098800     MOVE "START-TEST-GF-02" TO PAR-NAME.                         IX2154.2
098900     MOVE    "FGGGGGGGGG098" TO FDW-RECKEY-1-13.                  IX2154.2
099000     MOVE    WRK-FDW-RECKEY TO IX-REC-KEY-AREA.                   IX2154.2
099100 START-TEST-GF-02.                                                IX2154.2
099200     DELETE  IX-FD1 INVALID KEY                                   IX2154.2
099300             MOVE  "INVALID KEY (DELETE)" TO COMPUTED-A           IX2154.2
099400             GO TO START-FAIL-GF-02.                              IX2154.2
099500     PERFORM START-INIT-FD1 THRU START-INIT-FD1-EXIT.             IX2154.2
099600     IF      INIT-FLAG NOT EQUAL ZERO                             IX2154.2
099700             GO TO START-DELETE-GF-02.                            IX2154.2
099800     MOVE    "FGGGGGGAAA002" TO FDW-RECKEY-1-13.                  IX2154.2
099900     MOVE    WRK-FDW-RECKEY TO IX-REC-KEY-AREA.                   IX2154.2
100000     START   IX-FD1                                               IX2154.2
100100                    KEY IS EQUAL TO R-RECKEY-1-7                  IX2154.2
100200          INVALID KEY    PERFORM PASS                             IX2154.2
100300             MOVE "OK.; INVALID KEY ON START OF DELETED RECORD"   IX2154.2
100400                  TO RE-MARK                                      IX2154.2
100500             GO TO START-WRITE-GF-02.                             IX2154.2
100600     READ    IX-FD1 NEXT RECORD  AT END                           IX2154.2
100700             MOVE "AT END ON READ" TO COMPUTED-A                  IX2154.2
100800             GO TO START-FAIL-GF-02.                              IX2154.2
100900     MOVE    IX-FD1R1-F-G-240  TO  FILE-RECORD-INFO (1).          IX2154.2
101000     MOVE     XRECORD-NUMBER (1)  TO COMPUTED-18V0.               IX2154.2
101100 START-FAIL-GF-02.                                                IX2154.2
101200     PERFORM FAIL.                                                IX2154.2
101300     MOVE    "INVALID KEY ON START" TO CORRECT-A.                 IX2154.2
101400     MOVE "IX-28 OR IX-36; WRONG RECORD NUMBER FOUND" TO RE-MARK. IX2154.2
101500     GO TO   START-WRITE-GF-02.                                   IX2154.2
101600 START-DELETE-GF-02.                                              IX2154.2
101700     PERFORM DE-LETE.                                             IX2154.2
101800 START-WRITE-GF-02.                                               IX2154.2
101900     PERFORM PRINT-DETAIL.                                        IX2154.2
102000 START-INIT-GF-03.                                                IX2154.2
102100     MOVE "----------" TO DUMMY-RECORD. PERFORM BLANK-LINE-PRINT. IX2154.2
102200     MOVE "START-TEST-GF-03" TO PAR-NAME.                         IX2154.2
102300     MOVE    "UUUUUUUUUU400" TO FDW-RECKEY-1-13.                  IX2154.2
102400     MOVE    WRK-FDW-RECKEY TO IX-REC-KEY-AREA.                   IX2154.2
102500 START-TEST-GF-03.                                                IX2154.2
102600     DELETE  IX-FD1 INVALID KEY                                   IX2154.2
102700             MOVE  "INVALID KEY (DELETE)" TO COMPUTED-A           IX2154.2
102800             GO TO START-FAIL-GF-03.                              IX2154.2
102900     PERFORM START-INIT-FD1 THRU START-INIT-FD1-EXIT.             IX2154.2
103000     IF      INIT-FLAG NOT EQUAL ZERO                             IX2154.2
103100             GO TO START-DELETE-GF-03.                            IX2154.2
103200     MOVE    "UUUUURRRRR000" TO FDW-RECKEY-1-13.                  IX2154.2
103300     MOVE    WRK-FDW-RECKEY TO IX-REC-KEY-AREA.                   IX2154.2
103400     START   IX-FD1                                               IX2154.2
103500                    KEY IS EQUAL TO R-RECKEY-1-5                  IX2154.2
103600          INVALID KEY    PERFORM PASS                             IX2154.2
103700             MOVE "OK.; INVALID KEY ON START OF DELETED RECORD"   IX2154.2
103800                  TO RE-MARK                                      IX2154.2
103900             GO TO START-WRITE-GF-03.                             IX2154.2
104000     READ    IX-FD1 NEXT RECORD  AT END                           IX2154.2
104100             MOVE "AT END ON READ" TO COMPUTED-A                  IX2154.2
104200             GO TO START-FAIL-GF-03.                              IX2154.2
104300     MOVE    IX-FD1R1-F-G-240  TO  FILE-RECORD-INFO (1).          IX2154.2
104400     MOVE     XRECORD-NUMBER (1)  TO COMPUTED-18V0.               IX2154.2
104500 START-FAIL-GF-03.                                                IX2154.2
104600     PERFORM FAIL.                                                IX2154.2
104700     MOVE    "INVALID KEY ON START" TO CORRECT-A.                 IX2154.2
104800     MOVE "WRONG RECORD NUMBER FOUND; IX-28 OR IX-36" TO RE-MARK. IX2154.2
104900     GO TO   START-WRITE-GF-03.                                   IX2154.2
105000 START-DELETE-GF-03.                                              IX2154.2
105100     PERFORM DE-LETE.                                             IX2154.2
105200 START-WRITE-GF-03.                                               IX2154.2
105300     PERFORM PRINT-DETAIL.                                        IX2154.2
105400 START-INIT-GF-04.                                                IX2154.2
105500     MOVE    "START REDF ALT-KEY-1" TO FEATURE.                   IX2154.2
105600     MOVE "----------" TO DUMMY-RECORD. PERFORM BLANK-LINE-PRINT. IX2154.2
105700     MOVE "START-TEST-GF-04" TO PAR-NAME.                         IX2154.2
105800     MOVE    "TUUUUUUUUU398"  TO FDW-RECKEY-1-13.                 IX2154.2
105900     MOVE    WRK-FDW-RECKEY   TO IX-REC-KEY-AREA.                 IX2154.2
106000 START-TEST-GF-04.                                                IX2154.2
106100     READ    IX-FD1                                               IX2154.2
106200                     KEY IS IX-FD1-KEY                            IX2154.2
106300         INVALID KEY                                              IX2154.2
106400     MOVE "ERROR IX-28 F2; INVALID KEY PATH TAKEN ON INITIAL READ"IX2154.2
106500                   TO RE-MARK                                     IX2154.2
106600             MOVE  "INVALID KEY ON READ" TO COMPUTED-A            IX2154.2
106700             GO TO START-FAIL-GF-04.                              IX2154.2
106800     MOVE    IX-FD1R1-F-G-240 TO FILE-RECORD-INFO (3).            IX2154.2
106900     IF      XRECORD-NUMBER (3) NOT EQUAL TO 199                  IX2154.2
107000     MOVE "WRONG RECORD NUMBER FOUND; IX-28 OR IX-36" TO RE-MARK  IX2154.2
107100             MOVE  XRECORD-NUMBER (3) TO COMPUTED-18V0            IX2154.2
107200             GO TO START-FAIL-GF-04.                              IX2154.2
107300     PERFORM START-INIT-FD1 THRU START-INIT-FD1-EXIT.             IX2154.2
107400     IF      INIT-FLAG NOT EQUAL ZERO                             IX2154.2
107500             GO TO START-DELETE-GF-04.                            IX2154.2
107600     MOVE    "EEEEEEEEEE000ALTKEY1" TO ALTERNATE-KEY1 (3).        IX2154.2
107700     MOVE    "WWWWWWWWWW400ALTKEY2" TO ALTERNATE-KEY2 (3).        IX2154.2
107800     MOVE    FILE-RECORD-INFO (3) TO IX-FD1R1-F-G-240.            IX2154.2
107900     REWRITE IX-FD1R1-F-G-240 INVALID KEY                         IX2154.2
108000             MOVE "ERROR IX-33; INVALID KEY PATH TAKEN ON REWRITE"IX2154.2
108100                    TO RE-MARK                                    IX2154.2
108200             MOVE  "INVALID KEY; REWRITE" TO COMPUTED-A           IX2154.2
108300             GO TO START-FAIL-GF-04.                              IX2154.2
108400     PERFORM START-INIT-FD1 THRU START-INIT-FD1-EXIT.             IX2154.2
108500     IF      INIT-FLAG NOT EQUAL ZERO                             IX2154.2
108600             GO TO START-FAIL-GF-04.                              IX2154.2
108700     MOVE    "EEEEEEEEEE000ALTKEY1"  TO FDW-ALTKEY1-1-20.         IX2154.2
108800     MOVE    WRK-FDW-ALTKEY1  TO IX-ALT-KEY1-AREA.                IX2154.2
108900     START   IX-FD1                                               IX2154.2
109000                    KEY IS EQUAL TO IX-REDF-ALTKEY1               IX2154.2
109100          INVALID KEY                                             IX2154.2
109200             MOVE "ERROR IX-36; INVALID KEY PATH TAKEN ON START"  IX2154.2
109300             TO RE-MARK                                           IX2154.2
109400             MOVE  "INVALID KEY ON START" TO COMPUTED-A           IX2154.2
109500             GO TO START-FAIL-GF-04.                              IX2154.2
109600     READ    IX-FD1 NEXT RECORD AT END                            IX2154.2
109700            MOVE "IX-28 F1; AT END ON READ AFTER START" TO RE-MARKIX2154.2
109800             MOVE "AT END ON READ" TO COMPUTED-A                  IX2154.2
109900             GO TO START-FAIL-GF-04.                              IX2154.2
110000     MOVE    IX-FD1R1-F-G-240  TO FILE-RECORD-INFO (1).           IX2154.2
110100     IF      XRECORD-NUMBER (1) EQUAL TO 199                      IX2154.2
110200             PERFORM PASS                                         IX2154.2
110300             GO TO START-WRITE-GF-04.                             IX2154.2
110400     MOVE    ALTERNATE-KEY1 (1)  TO WRK-FDW-ALTKEY1.              IX2154.2
110500     MOVE    FDW-ALTKEY1-1-20  TO COMPUTED-A.                     IX2154.2
110600     MOVE    "EEEEEEEEEE000ALTKEY1"  TO CORRECT-A.                IX2154.2
110700     MOVE "IX-28 F1; INCORRECT ALTERNATE RECORD KEY1"  TO RE-MARK.IX2154.2
110800     PERFORM PRINT-DETAIL.                                        IX2154.2
110900     MOVE   "WRONG RECORD NUMBER FOUND ON READ ALTERNATE REC KEY1"IX2154.2
111000             TO RE-MARK.                                          IX2154.2
111100     MOVE XRECORD-NUMBER (1) TO COMPUTED-18V0.                    IX2154.2
111200 START-FAIL-GF-04.                                                IX2154.2
111300     MOVE "START-TEST-GF-04" TO PAR-NAME.                         IX2154.2
111400     PERFORM FAIL.                                                IX2154.2
111500     MOVE    199  TO CORRECT-18V0.                                IX2154.2
111600     GO TO   START-WRITE-GF-04.                                   IX2154.2
111700 START-DELETE-GF-04.                                              IX2154.2
111800     PERFORM DE-LETE.                                             IX2154.2
111900 START-WRITE-GF-04.                                               IX2154.2
112000     PERFORM PRINT-DETAIL.                                        IX2154.2
112100 START-INIT-GF-05.                                                IX2154.2
112200     MOVE "----------" TO DUMMY-RECORD. PERFORM BLANK-LINE-PRINT. IX2154.2
112300     MOVE "START-TEST-GF-05" TO PAR-NAME.                         IX2154.2
112400     MOVE    "BCCCCCCCCC018"  TO FDW-RECKEY-1-13.                 IX2154.2
112500     MOVE    WRK-FDW-RECKEY   TO IX-REC-KEY-AREA.                 IX2154.2
112600 START-TEST-GF-05.                                                IX2154.2
112700     READ    IX-FD1                                               IX2154.2
112800                     KEY IS IX-FD1-KEY                            IX2154.2
112900         INVALID KEY                                              IX2154.2
113000             MOVE "ERROR IX-33; INVALID KEY PATH TAKEN ON READ"   IX2154.2
113100                    TO RE-MARK                                    IX2154.2
113200             MOVE  "INVALID KEY ON READ" TO COMPUTED-A            IX2154.2
113300             GO TO START-FAIL-GF-05.                              IX2154.2
113400     MOVE    IX-FD1R1-F-G-240 TO FILE-RECORD-INFO (3).            IX2154.2
113500     IF      XRECORD-NUMBER (3) NOT EQUAL TO 9                    IX2154.2
113600             MOVE 9 TO RECNO                                      IX2154.2
113700             PERFORM DISPLAY-RECORD-KEYS                          IX2154.2
113800     MOVE "WRONG RECORD NUMBER FOUND; IX-28 OR IX-36" TO RE-MARK  IX2154.2
113900             MOVE  XRECORD-NUMBER (3) TO COMPUTED-18V0            IX2154.2
114000             GO TO START-FAIL-GF-05.                              IX2154.2
114100     PERFORM START-INIT-FD1 THRU START-INIT-FD1-EXIT.             IX2154.2
114200     IF      INIT-FLAG NOT EQUAL ZERO                             IX2154.2
114300             GO TO START-DELETE-GF-05.                            IX2154.2
114400     MOVE    "AAAAAAAAAA400ALTKEY1" TO ALTERNATE-KEY1 (3).        IX2154.2
114500     MOVE    "ZZZZZZZZZZ002ALTKEY2" TO ALTERNATE-KEY2 (3).        IX2154.2
114600     MOVE    FILE-RECORD-INFO (3) TO IX-FD1R1-F-G-240.            IX2154.2
114700     REWRITE IX-FD1R1-F-G-240 INVALID KEY                         IX2154.2
114800      MOVE "IX-33;  INVALID KEY PATH TAKEN ON REWRITE" TO RE-MARK IX2154.2
114900             MOVE  "INVALID KEY; REWRITE" TO COMPUTED-A           IX2154.2
115000             GO TO START-FAIL-GF-05.                              IX2154.2
115100     PERFORM START-INIT-FD1 THRU START-INIT-FD1-EXIT.             IX2154.2
115200     IF      INIT-FLAG NOT EQUAL ZERO                             IX2154.2
115300             GO TO START-FAIL-GF-05.                              IX2154.2
115400     MOVE    "AAAAAANNNN200ALTKEY1"  TO FDW-ALTKEY1-1-20.         IX2154.2
115500     MOVE    WRK-FDW-ALTKEY1  TO IX-ALT-KEY1-AREA.                IX2154.2
115600     START   IX-FD1                                               IX2154.2
115700                    KEY IS EQUAL TO R-ALTKEY1-1-6                 IX2154.2
115800          INVALID KEY                                             IX2154.2
115900      MOVE "IX-36; INVALID KEY ON START OF REWRITTEN RECORD"      IX2154.2
116000             TO RE-MARK                                           IX2154.2
116100             MOVE  "INVALID KEY ON START" TO COMPUTED-A           IX2154.2
116200             GO TO START-FAIL-GF-05.                              IX2154.2
116300     READ    IX-FD1 NEXT RECORD AT END                            IX2154.2
116400      MOVE "IX-28 F1; AT END ON READ AFTER START" TO RE-MARK      IX2154.2
116500             MOVE "AT END ON READ" TO COMPUTED-A                  IX2154.2
116600             GO TO START-FAIL-GF-05.                              IX2154.2
116700     MOVE    IX-FD1R1-F-G-240  TO FILE-RECORD-INFO (1).           IX2154.2
116800     IF      XRECORD-NUMBER (1) EQUAL TO 9                        IX2154.2
116900             PERFORM PASS                                         IX2154.2
117000             GO TO START-WRITE-GF-05.                             IX2154.2
117100     MOVE    ALTERNATE-KEY1 (1)  TO WRK-FDW-ALTKEY1.              IX2154.2
117200     MOVE    FDW-ALTKEY1-1-20  TO COMPUTED-A.                     IX2154.2
117300     MOVE    "AAAAAAAAAA400ALTKEY1"  TO CORRECT-A.                IX2154.2
117400     MOVE    "INCORRECT ALTERNATE RECORD KEY1"  TO RE-MARK.       IX2154.2
117500     PERFORM PRINT-DETAIL.                                        IX2154.2
117600     MOVE "IX-28 F1;WRONG RECORD NUMB FOUND READ ALTERN. REC KEY1"IX2154.2
117700             TO RE-MARK.                                          IX2154.2
117800     MOVE    XRECORD-NUMBER (1) TO COMPUTED-18V0.                 IX2154.2
117900 START-FAIL-GF-05.                                                IX2154.2
118000     PERFORM FAIL.                                                IX2154.2
118100     MOVE    9  TO CORRECT-18V0.                                  IX2154.2
118200     GO TO   START-WRITE-GF-05.                                   IX2154.2
118300 START-DELETE-GF-05.                                              IX2154.2
118400     PERFORM DE-LETE.                                             IX2154.2
118500 START-WRITE-GF-05.                                               IX2154.2
118600     PERFORM PRINT-DETAIL.                                        IX2154.2
118700 START-INIT-GF-06.                                                IX2154.2
118800     MOVE "----------" TO DUMMY-RECORD. PERFORM BLANK-LINE-PRINT. IX2154.2
118900     MOVE "START-TEST-GF-06" TO PAR-NAME.                         IX2154.2
119000     MOVE    "GGGGGGGGGG100"  TO FDW-RECKEY-1-13.                 IX2154.2
119100     MOVE    WRK-FDW-RECKEY   TO IX-REC-KEY-AREA.                 IX2154.2
119200 START-TEST-GF-06.                                                IX2154.2
119300     READ    IX-FD1                                               IX2154.2
119400                     KEY IS IX-FD1-KEY                            IX2154.2
119500         INVALID KEY                                              IX2154.2
119600             MOVE "ERROR IX-28; INVALID KEY PATH TAKEN ON READ"   IX2154.2
119700                    TO RE-MARK                                    IX2154.2
119800             GO TO START-FAIL-GF-06.                              IX2154.2
119900     MOVE    IX-FD1R1-F-G-240 TO FILE-RECORD-INFO (3).            IX2154.2
120000     IF      XRECORD-NUMBER (3) NOT EQUAL TO 50                   IX2154.2
120100             MOVE 50 TO RECNO                                     IX2154.2
120200             MOVE  "WRONG RECORD FOUND ON INITIAL READ" TO RE-MARKIX2154.2
120300             MOVE  XRECORD-NUMBER (3) TO COMPUTED-18V0            IX2154.2
120400             GO TO START-FAIL-GF-06.                              IX2154.2
120500     PERFORM START-INIT-FD1 THRU START-INIT-FD1-EXIT.             IX2154.2
120600     IF      INIT-FLAG NOT EQUAL ZERO                             IX2154.2
120700             GO TO START-DELETE-GF-06.                            IX2154.2
120800     MOVE    "AAGGGGGGGG100ALTKEY1" TO ALTERNATE-KEY1 (3).        IX2154.2
120900     MOVE    "GGGGGGGGGG100ALTKEY2" TO ALTERNATE-KEY2 (3).        IX2154.2
121000     MOVE    FILE-RECORD-INFO (3) TO IX-FD1R1-F-G-240.            IX2154.2
121100     REWRITE IX-FD1R1-F-G-240 INVALID KEY                         IX2154.2
121200             MOVE "ERROR IX-33; INVALID KEY PATH TAKEN ON REWRITE"IX2154.2
121300                    TO RE-MARK                                    IX2154.2
121400             MOVE  "INVALID KEY; REWRITE" TO COMPUTED-A           IX2154.2
121500             GO TO START-FAIL-GF-06.                              IX2154.2
121600                                                                  IX2154.2
121700     IF      INIT-FLAG NOT EQUAL ZERO                             IX2154.2
121800             GO TO START-FAIL-GF-06.                              IX2154.2
121900     MOVE    "AAGGZZZZZZ100ALTKEY1"  TO FDW-ALTKEY1-1-20.         IX2154.2
122000     MOVE    WRK-FDW-ALTKEY1  TO IX-ALT-KEY1-AREA.                IX2154.2
122100     START   IX-FD1                                               IX2154.2
122200                    KEY IS EQUAL TO R-ALTKEY1-1-4                 IX2154.2
122300          INVALID KEY                                             IX2154.2
122400             MOVE "ERROR IX-36; INVALID KEY PATH TAKEN ON START"  IX2154.2
122500                    TO RE-MARK                                    IX2154.2
122600             MOVE "INVALID KEY ON START OF REWRITTEN RECORD"      IX2154.2
122700             TO RE-MARK                                           IX2154.2
122800             MOVE  "INVALID KEY ON START" TO COMPUTED-A           IX2154.2
122900             GO TO START-FAIL-GF-06.                              IX2154.2
123000     READ    IX-FD1 NEXT RECORD AT END                            IX2154.2
123100           MOVE "IX-28 F1; AT END ON READ AFTER START" TO RE-MARK IX2154.2
123200             MOVE "AT END ON READ" TO COMPUTED-A                  IX2154.2
123300             GO TO START-FAIL-GF-06.                              IX2154.2
123400     MOVE    IX-FD1R1-F-G-240  TO FILE-RECORD-INFO (1).           IX2154.2
123500     IF      XRECORD-NUMBER (1) EQUAL TO 50                       IX2154.2
123600             PERFORM PASS                                         IX2154.2
123700             GO TO START-WRITE-GF-06.                             IX2154.2
123800     MOVE    ALTERNATE-KEY1 (1)  TO WRK-FDW-ALTKEY1.              IX2154.2
123900     MOVE    FDW-ALTKEY1-1-20  TO COMPUTED-A.                     IX2154.2
124000     MOVE    "AAGGGGGGGG100ALTKEY1"  TO CORRECT-A.                IX2154.2
124100     MOVE "IX-28 OR IX-36; INCORR ALTERN RECORD KEY1" TO RE-MARK. IX2154.2
124200     PERFORM PRINT-DETAIL.                                        IX2154.2
124300     MOVE "WRONG RECORD NUMBER FOUND ON READ ALTERNATE REC KEY1"  IX2154.2
124400             TO RE-MARK.                                          IX2154.2
124500     MOVE    XRECORD-NUMBER (1) TO COMPUTED-18V0.                 IX2154.2
124600 START-FAIL-GF-06.                                                IX2154.2
124700     PERFORM FAIL.                                                IX2154.2
124800     MOVE    50  TO CORRECT-18V0.                                 IX2154.2
124900     GO TO   START-WRITE-GF-06.                                   IX2154.2
125000 START-DELETE-GF-06.                                              IX2154.2
125100     PERFORM DE-LETE.                                             IX2154.2
125200 START-WRITE-GF-06.                                               IX2154.2
125300     PERFORM PRINT-DETAIL.                                        IX2154.2
125400 START-INIT-GF-07.                                                IX2154.2
125500     MOVE    "START REDF ALT-KEY-2" TO FEATURE.                   IX2154.2
125600     MOVE "----------" TO DUMMY-RECORD. PERFORM BLANK-LINE-PRINT. IX2154.2
125700     MOVE "START-TEST-GF-07" TO PAR-NAME.                         IX2154.2
125800     MOVE    "DDDDDEEEEE050"  TO FDW-RECKEY-1-13.                 IX2154.2
125900     MOVE    WRK-FDW-RECKEY   TO IX-REC-KEY-AREA.                 IX2154.2
126000 START-TEST-GF-07.                                                IX2154.2
126100     READ    IX-FD1                                               IX2154.2
126200                     KEY IS IX-FD1-KEY                            IX2154.2
126300         INVALID KEY                                              IX2154.2
126400      MOVE "IX-28 F2; INVALID KEY PATH TAKEN ON INITIAL READ"     IX2154.2
126500                   TO RE-MARK                                     IX2154.2
126600             MOVE  "INVALID KEY ON READ" TO COMPUTED-A            IX2154.2
126700             GO TO START-FAIL-GF-07.                              IX2154.2
126800     MOVE    IX-FD1R1-F-G-240 TO FILE-RECORD-INFO (3).            IX2154.2
126900     IF      XRECORD-NUMBER (3) NOT EQUAL TO 25                   IX2154.2
127000             MOVE 25 TO RECNO                                     IX2154.2
127100             PERFORM DISPLAY-RECORD-KEYS                          IX2154.2
127200     MOVE "IX-28;F2 WRONG RECORD FOUND ON INITIAL READ" TO RE-MARKIX2154.2
127300             MOVE  XRECORD-NUMBER (3) TO COMPUTED-18V0            IX2154.2
127400             GO TO START-FAIL-GF-07.                              IX2154.2
127500     PERFORM START-INIT-FD1 THRU START-INIT-FD1-EXIT.             IX2154.2
127600     IF      INIT-FLAG NOT EQUAL ZERO                             IX2154.2
127700             GO TO START-DELETE-GF-07.                            IX2154.2
127800     MOVE    "BCBCBCBCBC200ALTKEY1" TO ALTERNATE-KEY1 (3).        IX2154.2
127900     MOVE    "CBCBCBCBCB100ALTKEY2" TO ALTERNATE-KEY2 (3).        IX2154.2
128000     MOVE    FILE-RECORD-INFO (3) TO IX-FD1R1-F-G-240.            IX2154.2
128100     REWRITE IX-FD1R1-F-G-240 INVALID KEY                         IX2154.2
128200     MOVE "IX-33;   INVALID KEY PATH TAKEN ON REWRITE" TO RE-MARK IX2154.2
128300             MOVE  "INVALID KEY; REWRITE" TO COMPUTED-A           IX2154.2
128400             GO TO START-FAIL-GF-07.                              IX2154.2
128500     PERFORM START-INIT-FD1 THRU START-INIT-FD1-EXIT.             IX2154.2
128600     IF      INIT-FLAG NOT EQUAL ZERO                             IX2154.2
128700             GO TO START-FAIL-GF-07.                              IX2154.2
128800     MOVE    "CBCBCBCBCB100ALTKEY2"  TO FDW-ALTKEY2-1-20.         IX2154.2
128900     MOVE    WRK-FDW-ALTKEY2  TO IX-ALT-KEY2-AREA.                IX2154.2
129000     START   IX-FD1                                               IX2154.2
129100                    KEY IS EQUAL TO IX-REDF-ALTKEY2               IX2154.2
129200          INVALID KEY                                             IX2154.2
129300     MOVE "IX-36;  INVALID KEY ON START OF REWRITTEN RECORD"      IX2154.2
129400             TO RE-MARK                                           IX2154.2
129500             MOVE  "INVALID KEY ON START" TO COMPUTED-A           IX2154.2
129600             GO TO START-FAIL-GF-07.                              IX2154.2
129700     READ    IX-FD1 NEXT RECORD AT END                            IX2154.2
129800      MOVE "IX-28; F1 AT END ON READ AFTER START" TO RE-MARK      IX2154.2
129900             MOVE "AT END ON READ" TO COMPUTED-A                  IX2154.2
130000             GO TO START-FAIL-GF-07.                              IX2154.2
130100     MOVE    IX-FD1R1-F-G-240  TO FILE-RECORD-INFO (1).           IX2154.2
130200     IF      XRECORD-NUMBER (1) EQUAL TO 25                       IX2154.2
130300             PERFORM PASS                                         IX2154.2
130400             GO TO START-WRITE-GF-07.                             IX2154.2
130500     MOVE    ALTERNATE-KEY2 (1)  TO WRK-FDW-ALTKEY2.              IX2154.2
130600     MOVE    FDW-ALTKEY2-1-20  TO COMPUTED-A.                     IX2154.2
130700     MOVE    "CBCBCBCBCB100ALTKEY2"  TO CORRECT-A.                IX2154.2
130800     MOVE "IX-28 OR IX-36; INCORR ALTERN RECORD KEY2" TO RE-MARK. IX2154.2
130900     PERFORM PRINT-DETAIL.                                        IX2154.2
131000     MOVE   "WRONG RECORD NUMBER FOUND ON READ ALTERNATE REC KEY2"IX2154.2
131100             TO RE-MARK.                                          IX2154.2
131200     MOVE    XRECORD-NUMBER (1) TO COMPUTED-18V0.                 IX2154.2
131300 START-FAIL-GF-07.                                                IX2154.2
131400     PERFORM FAIL.                                                IX2154.2
131500     MOVE    25  TO CORRECT-18V0.                                 IX2154.2
131600     GO TO   START-WRITE-GF-07.                                   IX2154.2
131700 START-DELETE-GF-07.                                              IX2154.2
131800     PERFORM DE-LETE.                                             IX2154.2
131900 START-WRITE-GF-07.                                               IX2154.2
132000     PERFORM PRINT-DETAIL.                                        IX2154.2
132100 START-INIT-GF-08.                                                IX2154.2
132200     MOVE "----------" TO DUMMY-RECORD. PERFORM BLANK-LINE-PRINT. IX2154.2
132300     MOVE "START-TEST-GF-08" TO PAR-NAME.                         IX2154.2
132400     MOVE    "RRRRSSSSSS352"  TO FDW-RECKEY-1-13.                 IX2154.2
132500     MOVE    WRK-FDW-RECKEY   TO IX-REC-KEY-AREA.                 IX2154.2
132600 START-TEST-GF-08.                                                IX2154.2
132700     READ    IX-FD1                                               IX2154.2
132800                     KEY IS IX-FD1-KEY                            IX2154.2
132900         INVALID KEY                                              IX2154.2
133000      MOVE "IX-28;F2 INVALID KEY PATH TAKEN ON INITIAL READ"      IX2154.2
133100          TO RE-MARK                                              IX2154.2
133200             MOVE  "INVALID KEY ON READ" TO COMPUTED-A            IX2154.2
133300             GO TO START-FAIL-GF-08.                              IX2154.2
133400     MOVE    IX-FD1R1-F-G-240 TO FILE-RECORD-INFO (3).            IX2154.2
133500     IF      XRECORD-NUMBER (3) NOT EQUAL TO 176                  IX2154.2
133600             MOVE 51 TO RECNO                                     IX2154.2
133700             PERFORM DISPLAY-RECORD-KEYS                          IX2154.2
133800     MOVE "IX-28;F2 WRONG RECORD FOUND ON INITIAL READ" TO RE-MARKIX2154.2
133900             MOVE  XRECORD-NUMBER (3) TO COMPUTED-18V0            IX2154.2
134000             GO TO START-FAIL-GF-08.                              IX2154.2
134100     PERFORM START-INIT-FD1 THRU START-INIT-FD1-EXIT.             IX2154.2
134200     IF      INIT-FLAG NOT EQUAL ZERO                             IX2154.2
134300             GO TO START-DELETE-GF-08.                            IX2154.2
134400     MOVE    "DCDCDCDCDC100ALTKEY1" TO ALTERNATE-KEY1 (3).        IX2154.2
134500     MOVE    "DCDCDCDCDC100ALTKEY2" TO ALTERNATE-KEY2 (3).        IX2154.2
134600     MOVE    FILE-RECORD-INFO (3) TO IX-FD1R1-F-G-240.            IX2154.2
134700     REWRITE IX-FD1R1-F-G-240 INVALID KEY                         IX2154.2
134800     MOVE "IX-33;   INVALID KEY PATH TAKEN ON REWRITE" TO RE-MARK IX2154.2
134900             MOVE  "INVALID KEY; REWRITE" TO COMPUTED-A           IX2154.2
135000             GO TO START-FAIL-GF-08.                              IX2154.2
135100     PERFORM START-INIT-FD1 THRU START-INIT-FD1-EXIT.             IX2154.2
135200     IF      INIT-FLAG NOT EQUAL ZERO                             IX2154.2
135300             GO TO START-FAIL-GF-08.                              IX2154.2
135400     MOVE    "DCDAAAAAAA250ALTKEY2"  TO FDW-ALTKEY2-1-20.         IX2154.2
135500     MOVE    WRK-FDW-ALTKEY2  TO IX-ALT-KEY2-AREA.                IX2154.2
135600     START   IX-FD1                                               IX2154.2
135700                    KEY IS EQUAL TO R-ALTKEY2-1-3                 IX2154.2
135800          INVALID KEY                                             IX2154.2
135900     MOVE "IX-36;  INVALID KEY ON START OF REWRITTEN RECORD"      IX2154.2
136000             TO RE-MARK                                           IX2154.2
136100             MOVE  "INVALID KEY ON START" TO COMPUTED-A           IX2154.2
136200             GO TO START-FAIL-GF-08.                              IX2154.2
136300     READ    IX-FD1 NEXT RECORD AT END                            IX2154.2
136400     MOVE "IX-28;F1 AT END ON READ AFTER START" TO RE-MARK        IX2154.2
136500             MOVE "AT END ON READ" TO COMPUTED-A                  IX2154.2
136600             GO TO START-FAIL-GF-08.                              IX2154.2
136700     MOVE    IX-FD1R1-F-G-240  TO FILE-RECORD-INFO (1).           IX2154.2
136800     IF      XRECORD-NUMBER (1) EQUAL TO 176                      IX2154.2
136900             PERFORM PASS                                         IX2154.2
137000             GO TO START-WRITE-GF-08.                             IX2154.2
137100     MOVE    ALTERNATE-KEY2 (1)  TO WRK-FDW-ALTKEY2.              IX2154.2
137200     MOVE    FDW-ALTKEY2-1-20  TO COMPUTED-A.                     IX2154.2
137300     MOVE    "DCDCDCDCDC100ALTKEY2"  TO CORRECT-A.                IX2154.2
137400     MOVE "IX-28 OR IX-36; INCORR ALTERN RECORD KEY2" TO RE-MARK. IX2154.2
137500     PERFORM PRINT-DETAIL.                                        IX2154.2
137600     MOVE   "WRONG RECORD NUMBER FOUND ON READ ALTERNATE REC KEY2"IX2154.2
137700             TO RE-MARK.                                          IX2154.2
137800     MOVE    XRECORD-NUMBER (1) TO COMPUTED-18V0.                 IX2154.2
137900 START-FAIL-GF-08.                                                IX2154.2
138000     PERFORM FAIL.                                                IX2154.2
138100     MOVE    176  TO CORRECT-18V0.                                IX2154.2
138200     GO TO   START-WRITE-GF-08.                                   IX2154.2
138300 START-DELETE-GF-08.                                              IX2154.2
138400     PERFORM DE-LETE.                                             IX2154.2
138500 START-WRITE-GF-08.                                               IX2154.2
138600     PERFORM PRINT-DETAIL.                                        IX2154.2
138700 START-INIT-GF-09.                                                IX2154.2
138800     MOVE "----------" TO DUMMY-RECORD. PERFORM BLANK-LINE-PRINT. IX2154.2
138900     MOVE "START-TEST-GF-09" TO PAR-NAME.                         IX2154.2
139000     MOVE    "BBBBBBCCCC008"  TO FDW-RECKEY-1-13.                 IX2154.2
139100     MOVE    WRK-FDW-RECKEY   TO IX-REC-KEY-AREA.                 IX2154.2
139200 START-TEST-GF-09.                                                IX2154.2
139300     READ    IX-FD1                                               IX2154.2
139400                     KEY IS IX-FD1-KEY                            IX2154.2
139500         INVALID KEY                                              IX2154.2
139600     MOVE "IX-28;F2 INVALID KEY PATH TAKEN ON INITIAL READ"       IX2154.2
139700                   TO RE-MARK                                     IX2154.2
139800             MOVE  "INVALID KEY ON READ" TO COMPUTED-A            IX2154.2
139900             MOVE 4 TO CORRECT-18V0                               IX2154.2
140000             GO TO START-FAIL-GF-09.                              IX2154.2
140100     MOVE    IX-FD1R1-F-G-240 TO FILE-RECORD-INFO (3).            IX2154.2
140200     IF      XRECORD-NUMBER (3) NOT EQUAL TO 4                    IX2154.2
140300             MOVE 4 TO RECNO                                      IX2154.2
140400             PERFORM DISPLAY-RECORD-KEYS                          IX2154.2
140500     MOVE "IX-28;F2 WRONG RECORD FOUND ON INITIAL READ" TO RE-MARKIX2154.2
140600             MOVE  XRECORD-NUMBER (3) TO COMPUTED-18V0            IX2154.2
140700             MOVE 4 TO CORRECT-18V0                               IX2154.2
140800             GO TO START-FAIL-GF-09.                              IX2154.2
140900     PERFORM START-INIT-FD1 THRU START-INIT-FD1-EXIT.             IX2154.2
141000     IF      INIT-FLAG NOT EQUAL ZERO                             IX2154.2
141100             GO TO START-DELETE-GF-09.                            IX2154.2
141200     MOVE    "CDCDCDCDCD100ALTKEY1" TO ALTERNATE-KEY1 (3).        IX2154.2
141300     MOVE    "DCDCDCDCDC100ALTKEY2" TO ALTERNATE-KEY2 (3).        IX2154.2
141400     MOVE    FILE-RECORD-INFO (3) TO IX-FD1R1-F-G-240.            IX2154.2
141500     REWRITE IX-FD1R1-F-G-240 INVALID KEY                         IX2154.2
141600     MOVE "IX-33;   INVALID KEY PATH TAKEN ON REWRITE" TO RE-MARK IX2154.2
141700             MOVE  "INVALID KEY; REWRITE" TO COMPUTED-A           IX2154.2
141800             MOVE 4 TO CORRECT-18V0                               IX2154.2
141900             GO TO START-FAIL-GF-09.                              IX2154.2
142000     PERFORM START-INIT-FD1 THRU START-INIT-FD1-EXIT.             IX2154.2
142100     IF      INIT-FLAG NOT EQUAL ZERO                             IX2154.2
142200             GO TO START-FAIL-GF-09.                              IX2154.2
142300     MOVE    "DCZZZZZZZZ400ALTKEY2"  TO FDW-ALTKEY2-1-20.         IX2154.2
142400     MOVE    WRK-FDW-ALTKEY2  TO IX-ALT-KEY2-AREA.                IX2154.2
142500     START   IX-FD1                                               IX2154.2
142600                    KEY IS EQUAL TO R-ALTKEY2-1-2                 IX2154.2
142700          INVALID KEY                                             IX2154.2
142800     MOVE "IX-36;  INVALID KEY ON START OF REWRITTEN RECORD"      IX2154.2
142900             TO RE-MARK                                           IX2154.2
143000             MOVE  "INVALID KEY ON START" TO COMPUTED-A           IX2154.2
143100             MOVE 176 TO CORRECT-18V0                             IX2154.2
143200             GO TO START-FAIL-GF-09.                              IX2154.2
143300     READ    IX-FD1 NEXT RECORD AT END                            IX2154.2
143400     MOVE "IX-28;F1 AT END ON READ AFTER START" TO RE-MARK        IX2154.2
143500             MOVE "AT END ON READ" TO COMPUTED-A                  IX2154.2
143600             MOVE 176 TO CORRECT-18V0                             IX2154.2
143700             GO TO START-FAIL-GF-09.                              IX2154.2
143800     MOVE    IX-FD1R1-F-G-240  TO FILE-RECORD-INFO (1).           IX2154.2
143900     IF      XRECORD-NUMBER (1) NOT EQUAL TO 176                  IX2154.2
144000             MOVE ALTERNATE-KEY2 (1) TO WRK-FDW-ALTKEY2           IX2154.2
144100             MOVE FDW-ALTKEY2-1-20 TO COMPUTED-A                  IX2154.2
144200             MOVE "DCDCDCDCDC100" TO CORRECT-A                    IX2154.2
144300     MOVE                                                         IX2154.2
144400     "IX-28 OR IX-36; INCORR KEY FOUND ON FIRST READ DUPL KEYS"   IX2154.2
144500             TO RE-MARK                                           IX2154.2
144600             PERFORM PRINT-DETAIL                                 IX2154.2
144700     MOVE "WRONG RECORD NUMBER FOUND ON FIRST READ DUPLICATE KEYS"IX2154.2
144800             TO RE-MARK                                           IX2154.2
144900             MOVE XRECORD-NUMBER (1) TO COMPUTED-18V0             IX2154.2
145000             MOVE 176 TO CORRECT-18V0                             IX2154.2
145100             GO TO START-FAIL-GF-09.                              IX2154.2
145200     READ    IX-FD1 NEXT RECORD AT END                            IX2154.2
145300     MOVE "IX-28;F1 AT END ON READ AFTER FIRST READ" TO RE-MARK   IX2154.2
145400             MOVE "AT END ON READ" TO COMPUTED-A                  IX2154.2
145500             MOVE 4 TO CORRECT-18V0                               IX2154.2
145600             GO TO START-FAIL-GF-09.                              IX2154.2
145700     MOVE    IX-FD1R1-F-G-240 TO FILE-RECORD-INFO (1).            IX2154.2
145800     IF      XRECORD-NUMBER (1) EQUAL TO 4                        IX2154.2
145900             PERFORM PASS                                         IX2154.2
146000             PERFORM PRINT-DETAIL                                 IX2154.2
146100             GO TO START-WRITE-GF-09.                             IX2154.2
146200     MOVE    ALTERNATE-KEY2 (1)  TO WRK-FDW-ALTKEY2.              IX2154.2
146300     MOVE    FDW-ALTKEY1-1-20  TO COMPUTED-A.                     IX2154.2
146400     MOVE    "DCDCDCDCDC100ALTKEY2"  TO CORRECT-A.                IX2154.2
146500     MOVE                                                         IX2154.2
146600     "IX-28OR IX-36; INCORR KEY FOUND ON SECOND READ DUPL.  KEYS" IX2154.2
146700             TO RE-MARK.                                          IX2154.2
146800     PERFORM PRINT-DETAIL.                                        IX2154.2
146900     MOVE    XRECORD-NUMBER (1) TO COMPUTED-18V0.                 IX2154.2
147000     MOVE    4 TO CORRECT-18V0.                                   IX2154.2
147100 START-FAIL-GF-09.                                                IX2154.2
147200     PERFORM FAIL.                                                IX2154.2
147300     GO TO   START-WRITE-GF-09.                                   IX2154.2
147400 START-DELETE-GF-09.                                              IX2154.2
147500     PERFORM DE-LETE.                                             IX2154.2
147600 START-WRITE-GF-09.                                               IX2154.2
147700     PERFORM PRINT-DETAIL.                                        IX2154.2
147800 START-TERM-003.                                                  IX2154.2
147900     CLOSE   IX-FD1.                                              IX2154.2
148000 WRITE-INT-GF-02.                                                 IX2154.2
148100     OPEN    OUTPUT IX-FD2.                                       IX2154.2
148200     MOVE    "IX-FD2" TO XFILE-NAME           (1).                IX2154.2
148300     MOVE    "R1-F-G" TO  XRECORD-NAME        (1).                IX2154.2
148400     MOVE    ZERO     TO  XRECORD-NUMBER      (1).                IX2154.2
148500     MOVE    "RC"     TO  CHARS-OR-RECORDS    (1).                IX2154.2
148600     MOVE "IX215"     TO  XPROGRAM-NAME       (1).                IX2154.2
148700     MOVE    241      TO  XRECORD-LENGTH      (1).                IX2154.2
148800     MOVE    004      TO  XBLOCK-SIZE         (1).                IX2154.2
148900     MOVE    "IX"     TO  XFILE-ORGANIZATION  (1).                IX2154.2
149000     MOVE    "S"      TO  XLABEL-TYPE         (1).                IX2154.2
149100     MOVE    200      TO  RECORDS-IN-FILE     (1).                IX2154.2
149200     MOVE    "CREATE-FILE-FD2" TO FEATURE.                        IX2154.2
149300     MOVE    "WRITE-TEST-GF-02" TO PAR-NAME.                      IX2154.2
149400     MOVE    ZERO TO KEYSUB.                                      IX2154.2
149500     MOVE    ZERO TO INVKEY-COUNTER.                              IX2154.2
149600     MOVE    ZERO TO EXCUT-COUNTER-06V00.                         IX2154.2
149700 WRITE-INIT-GF-02-01.                                             IX2154.2
149800     PERFORM WRITE-TEST-GF-02-R1 50 TIMES.                        IX2154.2
149900     PERFORM WRITE-TEST-GF-02-R2 125 TIMES.                       IX2154.2
150000     PERFORM WRITE-TEST-GF-02-R1 25 TIMES.                        IX2154.2
150100     GO TO   WRITE-TEST-GF-02.                                    IX2154.2
150200 WRITE-TEST-GF-02-R1.                                             IX2154.2
150300     ADD     001  TO XRECORD-NUMBER (1).                          IX2154.2
150400     ADD     001  TO  KEYSUB.                                     IX2154.2
150500     MOVE    RECKEY-VALUE (KEYSUB) TO FDW-RECKEY-1-13.            IX2154.2
150600     MOVE    ALTKEY1-VALUE (KEYSUB) TO FDW-ALTKEY1-1-20.          IX2154.2
150700     MOVE    ALTKEY2-VALUE (KEYSUB) TO FDW-ALTKEY2-1-20.          IX2154.2
150800     MOVE    WRK-FDW-RECKEY   TO    XRECORD-KEY (1).              IX2154.2
150900     MOVE    WRK-FDW-ALTKEY1  TO ALTERNATE-KEY1 (1).              IX2154.2
151000     MOVE    WRK-FDW-ALTKEY2  TO ALTERNATE-KEY2 (1).              IX2154.2
151100     MOVE    FILE-RECORD-INFO (1)  TO IX-FD2R1-F-G-241.           IX2154.2
151200     WRITE   IX-FD2R1-F-G-241                                     IX2154.2
151300                        INVALID KEY ADD 001 TO INVKEY-COUNTER.    IX2154.2
151400     ADD     001 TO  EXCUT-COUNTER-06V00.                         IX2154.2
151500 WRITE-TEST-GF-02-R2.                                             IX2154.2
151600     ADD     002  TO FDW-RECKEY-11-13.                            IX2154.2
151700     ADD     002 TO FDW-ALTKEY1-11-13.                            IX2154.2
151800     SUBTRACT  002 FROM FDW-ALTKEY2-11-13.                        IX2154.2
151900     ADD     001  TO XRECORD-NUMBER (1).                          IX2154.2
152000     MOVE    WRK-FDW-RECKEY   TO    XRECORD-KEY (1).              IX2154.2
152100     MOVE    WRK-FDW-ALTKEY1  TO ALTERNATE-KEY1 (1).              IX2154.2
152200     MOVE    WRK-FDW-ALTKEY2  TO ALTERNATE-KEY2 (1).              IX2154.2
152300     MOVE    FILE-RECORD-INFO (1) TO IX-FD2R1-F-G-241.            IX2154.2
152400     WRITE   IX-FD2R1-F-G-241                                     IX2154.2
152500                        INVALID KEY ADD 001 TO INVKEY-COUNTER.    IX2154.2
152600     ADD     001 TO EXCUT-COUNTER-06V00.                          IX2154.2
152700 WRITE-TEST-GF-02.                                                IX2154.2
152800     SUBTRACT INVKEY-COUNTER FROM EXCUT-COUNTER-06V00             IX2154.2
152900             GIVING RECORDS-WRITTEN.                              IX2154.2
153000     IF      RECORDS-WRITTEN EQUAL TO 200                         IX2154.2
153100             PERFORM  PASS                                        IX2154.2
153200             MOVE "FILE IX-FD2 CREATED (200 RECORDS)" TO RE-MARK  IX2154.2
153300     ELSE    PERFORM FAIL                                         IX2154.2
153400             MOVE                                          "IX-41;IX2154.2
153500-       "WRONG NUMBER OF RECORDS WRITTEN (MAY ALREADY EXIST)"     IX2154.2
153600             TO RE-MARK                                           IX2154.2
153700             MOVE 200 TO CORRECT-18V0                             IX2154.2
153800             MOVE RECORDS-WRITTEN TO COMPUTED-18V0.               IX2154.2
153900     GO TO   WRITE-TEST-GF-02-END.                                IX2154.2
154000 WRITE-DELETE-GF-02.                                              IX2154.2
154100     PERFORM DE-LETE.                                             IX2154.2
154200 WRITE-TEST-GF-02-END.                                            IX2154.2
154300     PERFORM  PRINT-DETAIL.                                       IX2154.2
154400     CLOSE   IX-FD2.                                              IX2154.2
154500 READ-INIT-F1-02.                                                 IX2154.2
154600     OPEN    INPUT  IX-FD2.                                       IX2154.2
154700     MOVE    "READ-TEST-F1-02" TO PAR-NAME.                       IX2154.2
154800     MOVE    "READ FILE IX-FD2" TO FEATURE.                       IX2154.2
154900     MOVE    ZERO TO EXCUT-COUNTER-06V00.                         IX2154.2
155000     MOVE    02   TO RECKEY-NUM.                                  IX2154.2
155100     MOVE    002 TO ALTKEY1-NUM.                                  IX2154.2
155200 READ-TEST-F1-02-R1.                                              IX2154.2
155300     READ    IX-FD2 NEXT RECORD AT END  GO TO READ-TEST-F1-02.    IX2154.2
155400     MOVE    IX-FD2-KEY TO FDW-RECKEY-1-13.                       IX2154.2
155500     MOVE    IX-FD2-ALTKEY1 TO FDW-ALTKEY1-1-20.                  IX2154.2
155600     IF      FDW-RECKEY-11-13 EQUAL TO RECKEY-NUM                 IX2154.2
155700             AND FDW-ALTKEY1-11-13 EQUAL TO ALTKEY1-NUM           IX2154.2
155800             NEXT SENTENCE                                        IX2154.2
155900             ELSE                                                 IX2154.2
156000             PERFORM  READ-FAIL-F1-02.                            IX2154.2
156100     IF      EXCUT-COUNTER-06V00 NOT LESS THAN 200                IX2154.2
156200             GO TO READ-TEST-F1-02.                               IX2154.2
156300     ADD     001  TO EXCUT-COUNTER-06V00.                         IX2154.2
156400     ADD     002  TO RECKEY-NUM                                   IX2154.2
156500     ADD     002  TO ALTKEY1-NUM.                                 IX2154.2
156600     GO TO   READ-TEST-F1-02-R1.                                  IX2154.2
156700 READ-TEST-F1-02.                                                 IX2154.2
156800     IF      FAIL-SW EQUAL TO 1 GO TO READ-EXIT-F1-02.            IX2154.2
156900     IF      EXCUT-COUNTER-06V00 EQUAL TO 200                     IX2154.2
157000             PERFORM PASS                                         IX2154.2
157100             MOVE "200 RECORDS VERIFIED" TO RE-MARK               IX2154.2
157200     ELSE    PERFORM FAIL                                         IX2154.2
157300             MOVE                                                 IX2154.2
157400          "INCORRECT NUMBER OF RECORDS; IX-28 OR IX-41" TO RE-MARKIX2154.2
157500             MOVE 200 TO CORRECT-18V0                             IX2154.2
157600             MOVE EXCUT-COUNTER-06V00 TO COMPUTED-18V0.           IX2154.2
157700     PERFORM PRINT-DETAIL.                                        IX2154.2
157800     GO TO   READ-EXIT-F1-02.                                     IX2154.2
157900 READ-FAIL-F1-02.                                                 IX2154.2
158000     MOVE 1 TO FAIL-SW.                                           IX2154.2
158100     PERFORM FAIL.                                                IX2154.2
158200     MOVE    FDW-RECKEY-11-13  TO COMPUTED-18V0.                  IX2154.2
158300     MOVE    RECKEY-NUM  TO CORRECT-18V0.                         IX2154.2
158400     MOVE "NUM EMBEDDED IN RECKEY; IX-28 OR IX-41"  TO RE-MARK.   IX2154.2
158500     PERFORM PRINT-DETAIL.                                        IX2154.2
158600 READ-EXIT-F1-02.                                                 IX2154.2
158700     CLOSE   IX-FD2.                                              IX2154.2
158800 START-INIT-GF-10.                                                IX2154.2
158900     OPEN    I-O  IX-FD2.                                         IX2154.2
159000     MOVE "----------" TO DUMMY-RECORD. PERFORM BLANK-LINE-PRINT. IX2154.2
159100     MOVE "START-TEST-GF-10" TO PAR-NAME.                         IX2154.2
159200     MOVE    "START REDF REC-KEY" TO FEATURE.                     IX2154.2
159300     MOVE    "TTTTUUUUUU392" TO IX-FD2-KEY.                       IX2154.2
159400 START-TEST-GF-10.                                                IX2154.2
159500     DELETE  IX-FD2 INVALID KEY                                   IX2154.2
159600             MOVE  "INVALID KEY (DELETE)" TO COMPUTED-A           IX2154.2
159700             GO TO START-FAIL-GF-10.                              IX2154.2
159800     PERFORM START-INIT-FD2 THRU START-INIT-FD2-EXIT.             IX2154.2
159900     IF      INIT-FLAG NOT EQUAL ZERO                             IX2154.2
160000             GO TO START-DELETE-GF-10.                            IX2154.2
160100     MOVE    "TTTTUUUUUU392" TO IX-FD2-KEY.                       IX2154.2
160200     START   IX-FD2 KEY IS EQUAL TO IX-FD2-KEY                    IX2154.2
160300             INVALID KEY  PERFORM PASS                            IX2154.2
160400             GO TO START-WRITE-GF-10.                             IX2154.2
160500     READ    IX-FD2 NEXT RECORD  AT END                           IX2154.2
160600             MOVE "AT END ON READ" TO COMPUTED-A                  IX2154.2
160700             GO TO START-FAIL-GF-10.                              IX2154.2
160800     MOVE    IX-FD2-REC-240    TO  FILE-RECORD-INFO (1).          IX2154.2
160900     MOVE     XRECORD-NUMBER (1)  TO COMPUTED-18V0.               IX2154.2
161000 START-FAIL-GF-10.                                                IX2154.2
161100     PERFORM FAIL.                                                IX2154.2
161200     MOVE    "INVALID KEY ON START" TO CORRECT-A.                 IX2154.2
161300     MOVE "IX-36; WRONG RECORD NUMBER FOUND" TO RE-MARK.          IX2154.2
161400     GO TO   START-WRITE-GF-10.                                   IX2154.2
161500 START-DELETE-GF-10.                                              IX2154.2
161600     PERFORM DE-LETE.                                             IX2154.2
161700 START-WRITE-GF-10.                                               IX2154.2
161800     PERFORM PRINT-DETAIL.                                        IX2154.2
161900 START-INIT-GF-11.                                                IX2154.2
162000     MOVE "----------" TO DUMMY-RECORD. PERFORM BLANK-LINE-PRINT. IX2154.2
162100     MOVE "START-TEST-GF-11" TO PAR-NAME.                         IX2154.2
162200     MOVE    "FFFFFGGGGG090" TO IX-FD2-KEY.                       IX2154.2
162300 START-TEST-GF-11.                                                IX2154.2
162400     DELETE  IX-FD2 INVALID KEY                                   IX2154.2
162500             MOVE  "INVALID KEY (DELETE)" TO COMPUTED-A           IX2154.2
162600             GO TO START-FAIL-GF-11.                              IX2154.2
162700     PERFORM START-INIT-FD2 THRU START-INIT-FD2-EXIT.             IX2154.2
162800     IF      INIT-FLAG NOT EQUAL ZERO                             IX2154.2
162900             GO TO START-DELETE-GF-11.                            IX2154.2
163000     MOVE    "FFFFFGGGGG090" TO IX-FD2-KEY.                       IX2154.2
163100     START   IX-FD2 KEY IS EQUAL TO IX-FD2-RECKEY-REDF            IX2154.2
163200             INVALID KEY  PERFORM PASS                            IX2154.2
163300     MOVE "OK.;    INVALID KEY ON START OF DELETED RECORD"        IX2154.2
163400                  TO RE-MARK                                      IX2154.2
163500             GO TO START-WRITE-GF-11.                             IX2154.2
163600     READ    IX-FD2 NEXT RECORD  AT END                           IX2154.2
163700             MOVE "AT END ON READ" TO COMPUTED-A                  IX2154.2
163800             GO TO START-FAIL-GF-11.                              IX2154.2
163900     MOVE    IX-FD2-REC-240    TO  FILE-RECORD-INFO (1).          IX2154.2
164000     MOVE     XRECORD-NUMBER (1)  TO COMPUTED-18V0.               IX2154.2
164100 START-FAIL-GF-11.                                                IX2154.2
164200     PERFORM FAIL.                                                IX2154.2
164300     MOVE    "INVALID KEY ON START" TO CORRECT-A.                 IX2154.2
164400     MOVE "IX-28 OR IX-36; WRONG RECORD NUMBER FOUND" TO RE-MARK. IX2154.2
164500     GO TO   START-WRITE-GF-11.                                   IX2154.2
164600 START-DELETE-GF-11.                                              IX2154.2
164700     PERFORM DE-LETE.                                             IX2154.2
164800 START-WRITE-GF-11.                                               IX2154.2
164900     PERFORM PRINT-DETAIL.                                        IX2154.2
165000 START-INIT-GF-12.                                                IX2154.2
165100     MOVE "----------" TO DUMMY-RECORD. PERFORM BLANK-LINE-PRINT. IX2154.2
165200     MOVE "START-TEST-GF-12" TO PAR-NAME.                         IX2154.2
165300     MOVE    "BBBBCCCCCC012" TO IX-FD2-KEY.                       IX2154.2
165400 START-TEST-GF-12.                                                IX2154.2
165500     DELETE  IX-FD2 INVALID KEY                                   IX2154.2
165600             MOVE  "INVALID KEY (DELETE)" TO COMPUTED-A           IX2154.2
165700             GO TO START-FAIL-GF-12.                              IX2154.2
165800     PERFORM START-INIT-FD2 THRU START-INIT-FD2-EXIT.             IX2154.2
165900     IF      INIT-FLAG NOT EQUAL ZERO                             IX2154.2
166000             GO TO START-DELETE-GF-12.                            IX2154.2
166100     MOVE    "BBBBCCDDDD015" TO IX-FD2-KEY.                       IX2154.2
166200     START   IX-FD2                                               IX2154.2
166300                    KEY IS EQUAL TO IX-FD2-RECKEY-1-6             IX2154.2
166400          INVALID KEY    PERFORM PASS                             IX2154.2
166500             MOVE "OK.; INVALID KEY ON START OF DELETED RECORD"   IX2154.2
166600                  TO RE-MARK                                      IX2154.2
166700             GO TO START-WRITE-GF-12.                             IX2154.2
166800     READ    IX-FD2 NEXT RECORD  AT END                           IX2154.2
166900             MOVE "AT END ON READ" TO COMPUTED-A                  IX2154.2
167000             GO TO START-FAIL-GF-12.                              IX2154.2
167100     MOVE    IX-FD2-REC-240    TO  FILE-RECORD-INFO (1).          IX2154.2
167200     MOVE     XRECORD-NUMBER (1)  TO COMPUTED-18V0.               IX2154.2
167300 START-FAIL-GF-12.                                                IX2154.2
167400     PERFORM FAIL.                                                IX2154.2
167500     MOVE    "INVALID KEY ON START" TO CORRECT-A.                 IX2154.2
167600     MOVE    "WRONG RECORD NUMBER FOUND" TO RE-MARK.              IX2154.2
167700     GO TO   START-WRITE-GF-12.                                   IX2154.2
167800 START-DELETE-GF-12.                                              IX2154.2
167900     PERFORM DE-LETE.                                             IX2154.2
168000 START-WRITE-GF-12.                                               IX2154.2
168100     PERFORM PRINT-DETAIL.                                        IX2154.2
168200 START-INIT-GF-13.                                                IX2154.2
168300     MOVE    "START REDF ALT-KEY-1" TO FEATURE.                   IX2154.2
168400     MOVE "----------" TO DUMMY-RECORD. PERFORM BLANK-LINE-PRINT. IX2154.2
168500     MOVE "START-TEST-GF-13" TO PAR-NAME.                         IX2154.2
168600     MOVE    "RSSSSSSSSS358"  TO IX-FD2-KEY.                      IX2154.2
168700 START-TEST-GF-13.                                                IX2154.2
168800     READ    IX-FD2                                               IX2154.2
168900                     KEY IS IX-FD2-KEY                            IX2154.2
169000         INVALID KEY                                              IX2154.2
169100     MOVE "IX-28;F2 INVALID KEY PATH TAKEN ON INITIAL READ"       IX2154.2
169200                   TO RE-MARK                                     IX2154.2
169300             MOVE  "INVALID KEY ON READ" TO COMPUTED-A            IX2154.2
169400             GO TO START-FAIL-GF-13.                              IX2154.2
169500     MOVE    IX-FD2-REC-240   TO FILE-RECORD-INFO (3).            IX2154.2
169600     IF      XRECORD-NUMBER (3) NOT EQUAL TO 179                  IX2154.2
169700             MOVE 54 TO RECNO                                     IX2154.2
169800             PERFORM DISPLAY-RECORD-KEYS                          IX2154.2
169900             MOVE  "WRONG RECORD FOUND ON INITIAL READ" TO RE-MARKIX2154.2
170000             MOVE  XRECORD-NUMBER (3) TO COMPUTED-18V0            IX2154.2
170100             GO TO START-FAIL-GF-13.                              IX2154.2
170200     PERFORM START-INIT-FD2 THRU START-INIT-FD2-EXIT.             IX2154.2
170300     IF      INIT-FLAG NOT EQUAL ZERO                             IX2154.2
170400             GO TO START-DELETE-GF-13.                            IX2154.2
170500     MOVE    "EEEEEEEEEE000ALTKEY1" TO ALTERNATE-KEY1 (3).        IX2154.2
170600     MOVE    "WWWWWWWWWW400ALTKEY2" TO ALTERNATE-KEY2 (3).        IX2154.2
170700     MOVE    FILE-RECORD-INFO (3) TO IX-FD2R1-F-G-241.            IX2154.2
170800     REWRITE IX-FD2R1-F-G-241 INVALID KEY                         IX2154.2
170900     MOVE "IX-33;   INVALID KEY PATH TAKEN ON REWRITE" TO RE-MARK IX2154.2
171000             MOVE  "INVALID KEY; REWRITE" TO COMPUTED-A           IX2154.2
171100             GO TO START-FAIL-GF-13.                              IX2154.2
171200     PERFORM START-INIT-FD2 THRU START-INIT-FD2-EXIT.             IX2154.2
171300     IF      INIT-FLAG NOT EQUAL ZERO                             IX2154.2
171400             GO TO START-FAIL-GF-13.                              IX2154.2
171500     MOVE    "EEEEEEEEEE000ALTKEY1"  TO IX-FD2-ALTKEY1.           IX2154.2
171600     START   IX-FD2                                               IX2154.2
171700                    KEY IS EQUAL TO IX-FD2-ALTKEY1                IX2154.2
171800          INVALID KEY                                             IX2154.2
171900     MOVE "IX-36;  INVALID KEY ON START OF REWRITTEN RECORD"      IX2154.2
172000             TO RE-MARK                                           IX2154.2
172100             MOVE  "INVALID KEY ON START" TO COMPUTED-A           IX2154.2
172200             GO TO START-FAIL-GF-13.                              IX2154.2
172300     READ    IX-FD2 NEXT RECORD AT END                            IX2154.2
172400     MOVE "IX-28;F1 AT END ON READ AFTER START" TO RE-MARK        IX2154.2
172500             MOVE "AT END ON READ" TO COMPUTED-A                  IX2154.2
172600             GO TO START-FAIL-GF-13.                              IX2154.2
172700     MOVE    IX-FD2-REC-240    TO FILE-RECORD-INFO (1).           IX2154.2
172800     IF      XRECORD-NUMBER (1) EQUAL TO 179                      IX2154.2
172900             PERFORM PASS                                         IX2154.2
173000             GO TO START-WRITE-GF-13.                             IX2154.2
173100     MOVE    ALTERNATE-KEY1 (1)  TO WRK-FDW-ALTKEY1.              IX2154.2
173200     MOVE    FDW-ALTKEY1-1-20  TO COMPUTED-A.                     IX2154.2
173300     MOVE    "EEEEEEEEEE000ALTKEY1"  TO CORRECT-A.                IX2154.2
173400     MOVE "IX-28 OR IX-36; INCORR ALTERN RECORD KEY1" TO RE-MARK. IX2154.2
173500     PERFORM PRINT-DETAIL.                                        IX2154.2
173600     MOVE   "WRONG RECORD NUMBER FOUND ON READ ALTERNATE REC KEY1"IX2154.2
173700             TO RE-MARK.                                          IX2154.2
173800     MOVE    XRECORD-NUMBER (1) TO COMPUTED-18V0.                 IX2154.2
173900 START-FAIL-GF-13.                                                IX2154.2
174000     PERFORM FAIL.                                                IX2154.2
174100     MOVE    179  TO CORRECT-18V0.                                IX2154.2
174200     GO TO   START-WRITE-GF-13.                                   IX2154.2
174300 START-DELETE-GF-13.                                              IX2154.2
174400     PERFORM DE-LETE.                                             IX2154.2
174500 START-WRITE-GF-13.                                               IX2154.2
174600     PERFORM PRINT-DETAIL.                                        IX2154.2
174700 START-INIT-GF-14.                                                IX2154.2
174800     MOVE "----------" TO DUMMY-RECORD. PERFORM BLANK-LINE-PRINT. IX2154.2
174900     MOVE "START-TEST-GF-14" TO PAR-NAME.                         IX2154.2
175000     MOVE    "TTUUUUUUUU396"  TO IX-FD2-KEY.                      IX2154.2
175100 START-TEST-GF-14.                                                IX2154.2
175200     READ    IX-FD2                                               IX2154.2
175300                     KEY IS IX-FD2-KEY                            IX2154.2
175400         INVALID KEY                                              IX2154.2
175500     MOVE "IX-28;F2 INVALID KEY PATH TAKEN ON INITIAL READ"       IX2154.2
175600                   TO RE-MARK                                     IX2154.2
175700             MOVE  "INVALID KEY ON READ" TO COMPUTED-A            IX2154.2
175800             GO TO START-FAIL-GF-14.                              IX2154.2
175900     MOVE    IX-FD2-REC-240   TO FILE-RECORD-INFO (3).            IX2154.2
176000     IF      XRECORD-NUMBER (3) NOT EQUAL TO 198                  IX2154.2
176100             MOVE 73 TO RECNO                                     IX2154.2
176200             PERFORM DISPLAY-RECORD-KEYS                          IX2154.2
176300     MOVE "IX-28;F2 WRONG RECORD FOUND ON INITIAL READ" TO RE-MARKIX2154.2
176400             MOVE  XRECORD-NUMBER (3) TO COMPUTED-18V0            IX2154.2
176500             GO TO START-FAIL-GF-14.                              IX2154.2
176600     PERFORM START-INIT-FD2 THRU START-INIT-FD2-EXIT.             IX2154.2
176700     IF      INIT-FLAG NOT EQUAL ZERO                             IX2154.2
176800             GO TO START-DELETE-GF-14.                            IX2154.2
176900     MOVE    "AAAAAAAAAA400ALTKEY1" TO ALTERNATE-KEY1 (3).        IX2154.2
177000     MOVE    "ZZZZZZZZZZ002ALTKEY2" TO ALTERNATE-KEY2 (3).        IX2154.2
177100     MOVE    FILE-RECORD-INFO (3) TO IX-FD2R1-F-G-241.            IX2154.2
177200     REWRITE IX-FD2R1-F-G-241 INVALID KEY                         IX2154.2
177300     MOVE "IX-33;   INVALID KEY PATH TAKEN ON REWRITE" TO RE-MARK IX2154.2
177400             MOVE  "INVALID KEY; REWRITE" TO COMPUTED-A           IX2154.2
177500             GO TO START-FAIL-GF-14.                              IX2154.2
177600     PERFORM START-INIT-FD2 THRU START-INIT-FD2-EXIT.             IX2154.2
177700     IF      INIT-FLAG NOT EQUAL ZERO                             IX2154.2
177800             GO TO START-FAIL-GF-14.                              IX2154.2
177900     MOVE    "AAAAAAAAAA400ALTKEY1"  TO IX-FD2-ALTKEY1.           IX2154.2
178000     START   IX-FD2                                               IX2154.2
178100                    KEY IS EQUAL TO IX-FD2-REDF-ALTKEY1           IX2154.2
178200             INVALID KEY                                          IX2154.2
178300     MOVE "IX-36;  INVALID KEY ON START OF REWRITTEN RECORD"      IX2154.2
178400             TO RE-MARK                                           IX2154.2
178500             MOVE  "INVALID KEY ON START" TO COMPUTED-A           IX2154.2
178600             GO TO START-FAIL-GF-14.                              IX2154.2
178700     READ    IX-FD2 NEXT RECORD AT END                            IX2154.2
178800     MOVE "IX-28;F1 AT END ON READ AFTER START" TO RE-MARK        IX2154.2
178900             MOVE "AT END ON READ" TO COMPUTED-A                  IX2154.2
179000             GO TO START-FAIL-GF-14.                              IX2154.2
179100     MOVE    IX-FD2-REC-240    TO FILE-RECORD-INFO (1).           IX2154.2
179200     IF      XRECORD-NUMBER (1) EQUAL TO 198                      IX2154.2
179300             PERFORM PASS                                         IX2154.2
179400             GO TO START-WRITE-GF-14.                             IX2154.2
179500     MOVE    ALTERNATE-KEY1 (1)  TO WRK-FDW-ALTKEY1.              IX2154.2
179600     MOVE    FDW-ALTKEY1-1-20  TO COMPUTED-A.                     IX2154.2
179700     MOVE    "AAAAAAAAAA400ALTKEY1"  TO CORRECT-A.                IX2154.2
179800     MOVE "IX-28 OR IX-36; INCORR ALTERN RECORD KEY1" TO RE-MARK. IX2154.2
179900     PERFORM PRINT-DETAIL.                                        IX2154.2
180000     MOVE   "WRONG RECORD NUMBER FOUND ON READ ALTERNATE REC KEY1"IX2154.2
180100             TO RE-MARK.                                          IX2154.2
180200     MOVE    XRECORD-NUMBER (1) TO COMPUTED-18V0.                 IX2154.2
180300 START-FAIL-GF-14.                                                IX2154.2
180400     PERFORM FAIL.                                                IX2154.2
180500     MOVE    198  TO CORRECT-18V0.                                IX2154.2
180600     GO TO   START-WRITE-GF-14.                                   IX2154.2
180700 START-DELETE-GF-14.                                              IX2154.2
180800     PERFORM DE-LETE.                                             IX2154.2
180900 START-WRITE-GF-14.                                               IX2154.2
181000     PERFORM PRINT-DETAIL.                                        IX2154.2
181100 START-INIT-GF-15.                                                IX2154.2
181200     MOVE "----------" TO DUMMY-RECORD. PERFORM BLANK-LINE-PRINT. IX2154.2
181300     MOVE "START-TEST-GF-15" TO PAR-NAME.                         IX2154.2
181400     MOVE    "BBBBBBBBBC002"  TO IX-FD2-KEY.                      IX2154.2
181500 START-TEST-GF-15.                                                IX2154.2
181600     READ    IX-FD2                                               IX2154.2
181700                     KEY IS IX-FD2-KEY                            IX2154.2
181800         INVALID KEY                                              IX2154.2
181900     MOVE "IX-28;F2 INVALID KEY PATH TAKEN ON INITIAL READ"       IX2154.2
182000                   TO RE-MARK                                     IX2154.2
182100             MOVE  "INVALID KEY ON READ" TO COMPUTED-A            IX2154.2
182200             GO TO START-FAIL-GF-15.                              IX2154.2
182300     MOVE    IX-FD2-REC-240   TO FILE-RECORD-INFO (3).            IX2154.2
182400     IF      XRECORD-NUMBER (3) NOT EQUAL TO 1                    IX2154.2
182500             MOVE 1 TO RECNO                                      IX2154.2
182600             PERFORM DISPLAY-RECORD-KEYS                          IX2154.2
182700     MOVE "IX-28;F2 WRONG RECORD FOUND ON INITIAL READ" TO RE-MARKIX2154.2
182800             MOVE  XRECORD-NUMBER (3) TO COMPUTED-18V0            IX2154.2
182900             GO TO START-FAIL-GF-15.                              IX2154.2
183000     PERFORM START-INIT-FD2 THRU START-INIT-FD2-EXIT.             IX2154.2
183100     IF      INIT-FLAG NOT EQUAL ZERO                             IX2154.2
183200             GO TO START-DELETE-GF-15.                            IX2154.2
183300     MOVE    "AAGGGGGGGG100ALTKEY1" TO ALTERNATE-KEY1 (3).        IX2154.2
183400     MOVE    "GGGGGGGGGG100ALTKEY2" TO ALTERNATE-KEY2 (3).        IX2154.2
183500     MOVE    FILE-RECORD-INFO (3) TO IX-FD2R1-F-G-241.            IX2154.2
183600     REWRITE IX-FD2R1-F-G-241 INVALID KEY                         IX2154.2
183700     MOVE "IX-33;   INVALID KEY PATH TAKEN ON REWRITE" TO RE-MARK IX2154.2
183800             MOVE  "INVALID KEY; REWRITE" TO COMPUTED-A           IX2154.2
183900             GO TO START-FAIL-GF-15.                              IX2154.2
184000     PERFORM START-INIT-FD2 THRU START-INIT-FD2-EXIT.             IX2154.2
184100     IF      INIT-FLAG NOT EQUAL ZERO                             IX2154.2
184200             GO TO START-FAIL-GF-15.                              IX2154.2
184300     MOVE    "AAGGGZZZZZ100ALTKEY1"  TO IX-FD2-ALTKEY1.           IX2154.2
184400     START   IX-FD2                                               IX2154.2
184500                    KEY IS EQUAL TO IX-FD2-ALTKEY1-1-5            IX2154.2
184600             INVALID KEY                                          IX2154.2
184700     MOVE "IX-36;  INVALID KEY ON START OF REWRITTEN RECORD"      IX2154.2
184800             TO RE-MARK                                           IX2154.2
184900             MOVE  "INVALID KEY ON START" TO COMPUTED-A           IX2154.2
185000             GO TO START-FAIL-GF-15.                              IX2154.2
185100     READ    IX-FD2 NEXT RECORD AT END                            IX2154.2
185200     MOVE "IX-28;F1 AT END ON READ AFTER START" TO RE-MARK        IX2154.2
185300             MOVE "AT END ON READ" TO COMPUTED-A                  IX2154.2
185400             GO TO START-FAIL-GF-15.                              IX2154.2
185500     MOVE    IX-FD2-REC-240    TO FILE-RECORD-INFO (1).           IX2154.2
185600     IF      XRECORD-NUMBER (1) EQUAL TO 1                        IX2154.2
185700             PERFORM PASS                                         IX2154.2
185800             GO TO START-WRITE-GF-15.                             IX2154.2
185900     MOVE    ALTERNATE-KEY1 (1)  TO WRK-FDW-ALTKEY1.              IX2154.2
186000     MOVE    FDW-ALTKEY1-1-20  TO COMPUTED-A.                     IX2154.2
186100     MOVE    "AAGGGGGGGG100ALTKEY1"  TO CORRECT-A.                IX2154.2
186200     MOVE "IX-28 OR IX-36; INCORR ALTERN RECORD KEY1" TO RE-MARK. IX2154.2
186300     PERFORM PRINT-DETAIL.                                        IX2154.2
186400     MOVE   "WRONG RECORD NUMBER FOUND ON READ ALTERNATE REC KEY1"IX2154.2
186500             TO RE-MARK.                                          IX2154.2
186600     MOVE    XRECORD-NUMBER (1) TO COMPUTED-18V0.                 IX2154.2
186700 START-FAIL-GF-15.                                                IX2154.2
186800     PERFORM FAIL.                                                IX2154.2
186900     MOVE    1  TO CORRECT-18V0.                                  IX2154.2
187000     GO TO   START-WRITE-GF-15.                                   IX2154.2
187100 START-DELETE-GF-15.                                              IX2154.2
187200     PERFORM DE-LETE.                                             IX2154.2
187300 START-WRITE-GF-15.                                               IX2154.2
187400     PERFORM PRINT-DETAIL.                                        IX2154.2
187500 START-INIT-GF-16.                                                IX2154.2
187600     MOVE    "START REDF ALT-KEY-2" TO FEATURE.                   IX2154.2
187700     MOVE "----------" TO DUMMY-RECORD. PERFORM BLANK-LINE-PRINT. IX2154.2
187800     MOVE "START-TEST-GF-16" TO PAR-NAME.                         IX2154.2
187900     MOVE    "SSSSSTTTTT370"  TO IX-FD2-KEY.                      IX2154.2
188000 START-TEST-GF-16.                                                IX2154.2
188100     READ    IX-FD2                                               IX2154.2
188200                     KEY IS IX-FD2-KEY                            IX2154.2
188300         INVALID KEY                                              IX2154.2
188400     MOVE "IX-28;F2 INVALID KEY PATH TAKEN ON INITIAL READ"       IX2154.2
188500                   TO RE-MARK                                     IX2154.2
188600             MOVE  "INVALID KEY ON READ" TO COMPUTED-A            IX2154.2
188700             GO TO START-FAIL-GF-16.                              IX2154.2
188800     MOVE    IX-FD2-REC-240   TO FILE-RECORD-INFO (3).            IX2154.2
188900     IF      XRECORD-NUMBER (3) NOT EQUAL TO 185                  IX2154.2
189000             MOVE 60 TO RECNO                                     IX2154.2
189100             PERFORM DISPLAY-RECORD-KEYS                          IX2154.2
189200     MOVE "IX-28;F2 WRONG RECORD FOUND ON INITIAL READ" TO RE-MARKIX2154.2
189300             MOVE  XRECORD-NUMBER (3) TO COMPUTED-18V0            IX2154.2
189400             GO TO START-FAIL-GF-16.                              IX2154.2
189500     PERFORM START-INIT-FD2 THRU START-INIT-FD2-EXIT.             IX2154.2
189600     IF      INIT-FLAG NOT EQUAL ZERO                             IX2154.2
189700             GO TO START-DELETE-GF-16.                            IX2154.2
189800     MOVE    "BCBCBCBCBC200ALTKEY1" TO ALTERNATE-KEY1 (3).        IX2154.2
189900     MOVE    "CBCBCBCBCB100ALTKEY2" TO ALTERNATE-KEY2 (3).        IX2154.2
190000     MOVE    FILE-RECORD-INFO (3) TO IX-FD2R1-F-G-241.            IX2154.2
190100     REWRITE IX-FD2R1-F-G-241 INVALID KEY                         IX2154.2
190200     MOVE "IX-33;   INVALID KEY PATH TAKEN ON REWRITE" TO RE-MARK IX2154.2
190300             MOVE  "INVALID KEY; REWRITE" TO COMPUTED-A           IX2154.2
190400             GO TO START-FAIL-GF-16.                              IX2154.2
190500     PERFORM START-INIT-FD2 THRU START-INIT-FD2-EXIT.             IX2154.2
190600     IF      INIT-FLAG NOT EQUAL ZERO                             IX2154.2
190700             GO TO START-FAIL-GF-16.                              IX2154.2
190800     MOVE    "CBCBCBCBCB100ALTKEY2"  TO IX-FD2-ALTKEY2.           IX2154.2
190900     START   IX-FD2                                               IX2154.2
191000                    KEY IS EQUAL TO IX-FD2-ALTKEY2                IX2154.2
191100          INVALID KEY                                             IX2154.2
191200     MOVE "IX-36;  INVALID KEY ON START OF REWRITTEN RECORD"      IX2154.2
191300             TO RE-MARK                                           IX2154.2
191400             MOVE  "INVALID KEY ON START" TO COMPUTED-A           IX2154.2
191500             GO TO START-FAIL-GF-16.                              IX2154.2
191600     READ    IX-FD2 NEXT RECORD AT END                            IX2154.2
191700     MOVE "IX-28;F1 AT END ON READ AFTER START" TO RE-MARK        IX2154.2
191800             MOVE "AT END ON READ" TO COMPUTED-A                  IX2154.2
191900             GO TO START-FAIL-GF-16.                              IX2154.2
192000     MOVE    IX-FD2-REC-240    TO FILE-RECORD-INFO (1).           IX2154.2
192100     IF      XRECORD-NUMBER (1) EQUAL TO 185                      IX2154.2
192200             PERFORM PASS                                         IX2154.2
192300             GO TO START-WRITE-GF-16.                             IX2154.2
192400     MOVE    ALTERNATE-KEY2 (1)  TO WRK-FDW-ALTKEY2.              IX2154.2
192500     MOVE    FDW-ALTKEY2-1-20  TO COMPUTED-A.                     IX2154.2
192600     MOVE    "CBCBCBCBCB100ALTKEY2"  TO CORRECT-A.                IX2154.2
192700     MOVE "IX-28 OR IX-36; INCORR ALTERN RECORD KEY2" TO RE-MARK. IX2154.2
192800     PERFORM PRINT-DETAIL.                                        IX2154.2
192900     MOVE   "WRONG RECORD NUMBER FOUND ON READ ALTERNATE REC KEY2"IX2154.2
193000             TO RE-MARK.                                          IX2154.2
193100     MOVE    XRECORD-NUMBER (1) TO COMPUTED-18V0.                 IX2154.2
193200 START-FAIL-GF-16.                                                IX2154.2
193300     PERFORM FAIL.                                                IX2154.2
193400     MOVE    185  TO CORRECT-18V0.                                IX2154.2
193500     GO TO   START-WRITE-GF-16.                                   IX2154.2
193600 START-DELETE-GF-16.                                              IX2154.2
193700     PERFORM DE-LETE.                                             IX2154.2
193800 START-WRITE-GF-16.                                               IX2154.2
193900     PERFORM PRINT-DETAIL.                                        IX2154.2
194000 START-INIT-GF-17.                                                IX2154.2
194100     MOVE "----------" TO DUMMY-RECORD. PERFORM BLANK-LINE-PRINT. IX2154.2
194200     MOVE "START-TEST-GF-17" TO PAR-NAME.                         IX2154.2
194300     MOVE    "FFFFFFFFFG082"  TO IX-FD2-KEY.                      IX2154.2
194400 START-TEST-GF-17.                                                IX2154.2
194500     READ    IX-FD2                                               IX2154.2
194600                     KEY IS IX-FD2-KEY                            IX2154.2
194700         INVALID KEY                                              IX2154.2
194800     MOVE "IX-28;F2 INVALID KEY PATH TAKEN ON INITIAL READ"       IX2154.2
194900                   TO RE-MARK                                     IX2154.2
195000             MOVE  "INVALID KEY ON READ" TO COMPUTED-A            IX2154.2
195100             GO TO START-FAIL-GF-17.                              IX2154.2
195200     MOVE    IX-FD2-REC-240   TO FILE-RECORD-INFO (3).            IX2154.2
195300     IF      XRECORD-NUMBER (3) NOT EQUAL TO 41                   IX2154.2
195400             MOVE 41 TO RECNO                                     IX2154.2
195500             PERFORM DISPLAY-RECORD-KEYS                          IX2154.2
195600     MOVE "IX-28;F2 WRONG RECORD FOUND ON INITIAL READ" TO RE-MARKIX2154.2
195700             MOVE  XRECORD-NUMBER (3) TO COMPUTED-18V0            IX2154.2
195800             GO TO START-FAIL-GF-17.                              IX2154.2
195900     PERFORM START-INIT-FD2 THRU START-INIT-FD2-EXIT.             IX2154.2
196000     IF      INIT-FLAG NOT EQUAL ZERO                             IX2154.2
196100             GO TO START-DELETE-GF-17.                            IX2154.2
196200     MOVE    "DCDCDCDCDC100ALTKEY1" TO ALTERNATE-KEY1 (3).        IX2154.2
196300     MOVE    "DCDCDCDCDC100ALTKEY2" TO ALTERNATE-KEY2 (3).        IX2154.2
196400     MOVE    FILE-RECORD-INFO (3) TO IX-FD2R1-F-G-241.            IX2154.2
196500     REWRITE IX-FD2R1-F-G-241 INVALID KEY                         IX2154.2
196600     MOVE "IX-33;   INVALID KEY PATH TAKEN ON REWRITE" TO RE-MARK IX2154.2
196700             MOVE  "INVALID KEY; REWRITE" TO COMPUTED-A           IX2154.2
196800             GO TO START-FAIL-GF-17.                              IX2154.2
196900     PERFORM START-INIT-FD2 THRU START-INIT-FD2-EXIT.             IX2154.2
197000     IF      INIT-FLAG NOT EQUAL ZERO                             IX2154.2
197100             GO TO START-FAIL-GF-17.                              IX2154.2
197200     MOVE    "DCDCDCZZZZ999ALTKEY2"  TO IX-FD2-ALTKEY2.           IX2154.2
197300     START   IX-FD2                                               IX2154.2
197400                    KEY IS EQUAL TO IX-FD2-ALTKEY2-1-6            IX2154.2
197500          INVALID KEY                                             IX2154.2
197600     MOVE "IX-36;  INVALID KEY ON START OF REWRITTEN RECORD"      IX2154.2
197700             TO RE-MARK                                           IX2154.2
197800             MOVE  "INVALID KEY ON START" TO COMPUTED-A           IX2154.2
197900             GO TO START-FAIL-GF-17.                              IX2154.2
198000     READ    IX-FD2 NEXT RECORD AT END                            IX2154.2
198100     MOVE "IX-28;F1 AT END ON READ AFTER START" TO RE-MARK        IX2154.2
198200             MOVE "AT END ON READ" TO COMPUTED-A                  IX2154.2
198300             GO TO START-FAIL-GF-17.                              IX2154.2
198400     MOVE    IX-FD2-REC-240    TO FILE-RECORD-INFO (1).           IX2154.2
198500     IF      XRECORD-NUMBER (1) EQUAL TO 41                       IX2154.2
198600             PERFORM PASS                                         IX2154.2
198700             GO TO START-WRITE-GF-17.                             IX2154.2
198800     MOVE    ALTERNATE-KEY2 (1)  TO WRK-FDW-ALTKEY2.              IX2154.2
198900     MOVE    FDW-ALTKEY2-1-20  TO COMPUTED-A.                     IX2154.2
199000     MOVE    "DCDCDCDCDC100ALTKEY2"  TO CORRECT-A.                IX2154.2
199100     MOVE "IX-28 OR IX-36; INCORR ALTERN RECORD KEY2" TO RE-MARK. IX2154.2
199200     PERFORM PRINT-DETAIL.                                        IX2154.2
199300     MOVE   "WRONG RECORD NUMBER FOUND ON READ ALTERNATE REC KEY2"IX2154.2
199400             TO RE-MARK.                                          IX2154.2
199500     MOVE    XRECORD-NUMBER (1) TO COMPUTED-18V0.                 IX2154.2
199600 START-FAIL-GF-17.                                                IX2154.2
199700     PERFORM FAIL.                                                IX2154.2
199800     MOVE    41  TO CORRECT-18V0.                                 IX2154.2
199900     GO TO   START-WRITE-GF-17.                                   IX2154.2
200000 START-DELETE-GF-17.                                              IX2154.2
200100     PERFORM DE-LETE.                                             IX2154.2
200200 START-WRITE-GF-17.                                               IX2154.2
200300     PERFORM PRINT-DETAIL.                                        IX2154.2
200400 START-INIT-GF-18.                                                IX2154.2
200500     MOVE "----------" TO DUMMY-RECORD. PERFORM BLANK-LINE-PRINT. IX2154.2
200600     MOVE "START-TEST-GF-18" TO PAR-NAME.                         IX2154.2
200700     MOVE    "TTTTTTTTUU384"  TO IX-FD2-KEY.                      IX2154.2
200800 START-TEST-GF-18.                                                IX2154.2
200900     READ    IX-FD2                                               IX2154.2
201000                     KEY IS IX-FD2-KEY                            IX2154.2
201100         INVALID KEY                                              IX2154.2
201200     MOVE "IX-28;F2 INVALID KEY PATH TAKEN ON INITIAL READ"       IX2154.2
201300                   TO RE-MARK                                     IX2154.2
201400             MOVE  "INVALID KEY ON READ" TO COMPUTED-A            IX2154.2
201500             MOVE 192 TO CORRECT-18V0                             IX2154.2
201600             GO TO START-FAIL-GF-18.                              IX2154.2
201700     MOVE    IX-FD2-REC-240   TO FILE-RECORD-INFO (3).            IX2154.2
201800     IF      XRECORD-NUMBER (3) NOT EQUAL TO 192                  IX2154.2
201900             MOVE 67 TO RECNO                                     IX2154.2
202000             PERFORM DISPLAY-RECORD-KEYS                          IX2154.2
202100     MOVE "IX-28;F2 WRONG RECORD FOUND ON INITIAL READ" TO RE-MARKIX2154.2
202200             MOVE  XRECORD-NUMBER (3) TO COMPUTED-18V0            IX2154.2
202300             MOVE 192 TO CORRECT-18V0                             IX2154.2
202400             GO TO START-FAIL-GF-18.                              IX2154.2
202500     PERFORM START-INIT-FD2 THRU START-INIT-FD2-EXIT.             IX2154.2
202600     IF      INIT-FLAG NOT EQUAL ZERO                             IX2154.2
202700             GO TO START-DELETE-GF-18.                            IX2154.2
202800     MOVE    "CDCDCDCDCD100ALTKEY1" TO ALTERNATE-KEY1 (3).        IX2154.2
202900     MOVE    "DCDCDCDCDC100ALTKEY2" TO ALTERNATE-KEY2 (3).        IX2154.2
203000     MOVE    FILE-RECORD-INFO (3) TO IX-FD2R1-F-G-241.            IX2154.2
203100     REWRITE IX-FD2R1-F-G-241 INVALID KEY                         IX2154.2
203200     MOVE "IX-33;   INVALID KEY PATH TAKEN ON REWRITE" TO RE-MARK IX2154.2
203300             MOVE  "INVALID KEY; REWRITE" TO COMPUTED-A           IX2154.2
203400             MOVE 192 TO CORRECT-18V0                             IX2154.2
203500             GO TO START-FAIL-GF-18.                              IX2154.2
203600     PERFORM START-INIT-FD2 THRU START-INIT-FD2-EXIT.             IX2154.2
203700     IF      INIT-FLAG NOT EQUAL ZERO                             IX2154.2
203800             GO TO START-FAIL-GF-18.                              IX2154.2
203900     MOVE    "DCDZZZZZZZ400ALTKEY2"  TO IX-FD2-ALTKEY2.           IX2154.2
204000     START   IX-FD2                                               IX2154.2
204100                    KEY IS EQUAL TO IX-FD2-ALTKEY2-1-3            IX2154.2
204200             INVALID KEY                                          IX2154.2
204300     MOVE "IX-36;  INVALID KEY ON START OF REWRITTEN RECORD"      IX2154.2
204400             TO RE-MARK                                           IX2154.2
204500             MOVE  "INVALID KEY ON START" TO COMPUTED-A           IX2154.2
204600             MOVE 41 TO CORRECT-18V0                              IX2154.2
204700             GO TO START-FAIL-GF-18.                              IX2154.2
204800     READ    IX-FD2 NEXT RECORD AT END                            IX2154.2
204900     MOVE "IX-28;F1 AT END ON READ AFTER START" TO RE-MARK        IX2154.2
205000             MOVE "AT END ON READ" TO COMPUTED-A                  IX2154.2
205100             MOVE 41 TO CORRECT-18V0                              IX2154.2
205200             GO TO START-FAIL-GF-18.                              IX2154.2
205300     MOVE    IX-FD2-REC-240    TO FILE-RECORD-INFO (1).           IX2154.2
205400     IF      XRECORD-NUMBER (1) NOT EQUAL TO 41                   IX2154.2
205500             MOVE ALTERNATE-KEY2 (1) TO WRK-FDW-ALTKEY2           IX2154.2
205600             MOVE FDW-ALTKEY2-1-20 TO COMPUTED-A                  IX2154.2
205700             MOVE "DCDCDCDCDC100" TO CORRECT-A                    IX2154.2
205800     MOVE                                                         IX2154.2
205900     "IX-28/36; INCORRECT  KEY FOUND ON FIRST READ DUPLICATE KEYS"IX2154.2
206000             TO RE-MARK                                           IX2154.2
206100             PERFORM PRINT-DETAIL                                 IX2154.2
206200     MOVE "WRONG RECORD NUMBER FOUND ON FIRST READ DUPLICATE KEYS"IX2154.2
206300             TO RE-MARK                                           IX2154.2
206400             MOVE XRECORD-NUMBER (1) TO COMPUTED-18V0             IX2154.2
206500             MOVE 41 TO CORRECT-18V0                              IX2154.2
206600             GO TO START-FAIL-GF-18.                              IX2154.2
206700     READ    IX-FD2 NEXT RECORD AT END                            IX2154.2
206800     MOVE "IX-28;F1 AT END ON READ AFTER FIRST READ" TO RE-MARK   IX2154.2
206900             MOVE "AT END ON READ" TO COMPUTED-A                  IX2154.2
207000             MOVE 192 TO CORRECT-18V0                             IX2154.2
207100             GO TO START-FAIL-GF-18.                              IX2154.2
207200     MOVE    IX-FD2-REC-240   TO FILE-RECORD-INFO (1).            IX2154.2
207300     IF      XRECORD-NUMBER (1) EQUAL TO 192                      IX2154.2
207400             PERFORM PASS                                         IX2154.2
207500             GO TO START-WRITE-GF-18.                             IX2154.2
207600     MOVE    ALTERNATE-KEY2 (1)  TO WRK-FDW-ALTKEY2.              IX2154.2
207700     MOVE    FDW-ALTKEY2-1-20  TO COMPUTED-A.                     IX2154.2
207800     MOVE    "DCDCDCDCDC100ALTKEY2"  TO CORRECT-A.                IX2154.2
207900     MOVE                                                         IX2154.2
208000     "IX-28/-36INCORRECT KEY FOUND ON SECOND READ DUPLICATE KEYS" IX2154.2
208100             TO RE-MARK.                                          IX2154.2
208200     PERFORM PRINT-DETAIL.                                        IX2154.2
208300     MOVE "WRONG REC NUMBER FOUND ON SECOND READ DUPLICATE KEYS"  IX2154.2
208400             TO RE-MARK.                                          IX2154.2
208500     MOVE    XRECORD-NUMBER (1) TO COMPUTED-18V0.                 IX2154.2
208600     MOVE    192 TO CORRECT-18V0.                                 IX2154.2
208700 START-FAIL-GF-18.                                                IX2154.2
208800     PERFORM FAIL.                                                IX2154.2
208900     GO TO   START-WRITE-GF-18.                                   IX2154.2
209000 START-DELETE-GF-18.                                              IX2154.2
209100     PERFORM DE-LETE.                                             IX2154.2
209200 START-WRITE-GF-18.                                               IX2154.2
209300     PERFORM PRINT-DETAIL.                                        IX2154.2
209400 WRITE-WRITE-03.                                                  IX2154.2
209500     CLOSE   IX-FD2.                                              IX2154.2
209600 WRITE-INT-GF-03.                                                 IX2154.2
209700     OPEN    OUTPUT IX-FD3.                                       IX2154.2
209800     MOVE    "IX-FD3" TO XFILE-NAME           (1).                IX2154.2
209900     MOVE    "R1-F-G" TO  XRECORD-NAME        (1).                IX2154.2
210000     MOVE    ZERO     TO  XRECORD-NUMBER      (1).                IX2154.2
210100     MOVE    "RC"     TO  CHARS-OR-RECORDS    (1).                IX2154.2
210200     MOVE "IX215"     TO  XPROGRAM-NAME       (1).                IX2154.2
210300     MOVE    242      TO  XRECORD-LENGTH      (1).                IX2154.2
210400     MOVE    007      TO  XBLOCK-SIZE         (1).                IX2154.2
210500     MOVE    "IX"     TO  XFILE-ORGANIZATION  (1).                IX2154.2
210600     MOVE    "S"      TO  XLABEL-TYPE         (1).                IX2154.2
210700     MOVE    200      TO  RECORDS-IN-FILE     (1).                IX2154.2
210800     MOVE    "CREATE-FILE-FD3" TO FEATURE.                        IX2154.2
210900     MOVE    "WRITE-TEST-GF-03" TO PAR-NAME.                      IX2154.2
211000     MOVE    ZERO TO KEYSUB.                                      IX2154.2
211100     MOVE    ZERO TO INVKEY-COUNTER.                              IX2154.2
211200     MOVE    ZERO TO EXCUT-COUNTER-06V00.                         IX2154.2
211300 WRITE-INIT-GF-03.                                                IX2154.2
211400     PERFORM WRITE-TEST-GF-03R1  50 TIMES.                        IX2154.2
211500     PERFORM WRITE-TEST-GF-03R2  125 TIMES.                       IX2154.2
211600     PERFORM WRITE-TEST-GF-03R1  25 TIMES.                        IX2154.2
211700     GO TO   WRITE-TEST-GF-03.                                    IX2154.2
211800 WRITE-TEST-GF-03R1.                                              IX2154.2
211900     ADD     001  TO XRECORD-NUMBER (1).                          IX2154.2
212000     ADD     001  TO  KEYSUB.                                     IX2154.2
212100     MOVE    RECKEY-VALUE (KEYSUB) TO FDW-RECKEY-1-13.            IX2154.2
212200     MOVE    ALTKEY1-VALUE (KEYSUB) TO FDW-ALTKEY1-1-20.          IX2154.2
212300     MOVE    ALTKEY2-VALUE (KEYSUB) TO FDW-ALTKEY2-1-20.          IX2154.2
212400     MOVE    WRK-FDW-RECKEY   TO    XRECORD-KEY (1).              IX2154.2
212500     MOVE    WRK-FDW-ALTKEY1  TO ALTERNATE-KEY1 (1).              IX2154.2
212600     MOVE    WRK-FDW-ALTKEY2  TO ALTERNATE-KEY2 (1).              IX2154.2
212700     MOVE    FILE-RECORD-INFO (1)  TO IX-FD3R1-F-G-242.           IX2154.2
212800     WRITE   IX-FD3R1-F-G-242                                     IX2154.2
212900                        INVALID KEY ADD 001 TO INVKEY-COUNTER.    IX2154.2
213000     ADD     001 TO  EXCUT-COUNTER-06V00.                         IX2154.2
213100 WRITE-TEST-GF-03R2.                                              IX2154.2
213200     ADD     002  TO FDW-RECKEY-11-13.                            IX2154.2
213300     ADD     002 TO FDW-ALTKEY1-11-13.                            IX2154.2
213400     SUBTRACT  002 FROM FDW-ALTKEY2-11-13.                        IX2154.2
213500     ADD     001  TO XRECORD-NUMBER (1).                          IX2154.2
213600     MOVE    WRK-FDW-RECKEY   TO    XRECORD-KEY (1).              IX2154.2
213700     MOVE    WRK-FDW-ALTKEY1  TO ALTERNATE-KEY1 (1).              IX2154.2
213800     MOVE    WRK-FDW-ALTKEY2  TO ALTERNATE-KEY2 (1).              IX2154.2
213900     MOVE    FILE-RECORD-INFO (1) TO IX-FD3R1-F-G-242.            IX2154.2
214000     WRITE   IX-FD3R1-F-G-242                                     IX2154.2
214100                        INVALID KEY ADD 001 TO INVKEY-COUNTER.    IX2154.2
214200     ADD     001 TO EXCUT-COUNTER-06V00.                          IX2154.2
214300 WRITE-TEST-GF-03.                                                IX2154.2
214400     SUBTRACT INVKEY-COUNTER FROM EXCUT-COUNTER-06V00             IX2154.2
214500             GIVING RECORDS-WRITTEN.                              IX2154.2
214600     IF      RECORDS-WRITTEN EQUAL TO 200                         IX2154.2
214700             PERFORM  PASS                                        IX2154.2
214800             MOVE "FILE IX-FD3 CREATED (200 RECORDS)" TO RE-MARK  IX2154.2
214900     ELSE    PERFORM FAIL                                         IX2154.2
215000             MOVE                                          "IX-41;IX2154.2
215100-       "WRONG NUMBER OF RECORDS WRITTEN (MAY ALREADY EXIST)"     IX2154.2
215200             TO RE-MARK                                           IX2154.2
215300             MOVE 200 TO CORRECT-18V0                             IX2154.2
215400             MOVE RECORDS-WRITTEN TO COMPUTED-18V0.               IX2154.2
215500     PERFORM PRINT-DETAIL.                                        IX2154.2
215600     GO TO   WRITE-TEST-GF-03-END.                                IX2154.2
215700 WRITE-DELETE-GF-03.                                              IX2154.2
215800     PERFORM DE-LETE.                                             IX2154.2
215900     PERFORM  PRINT-DETAIL.                                       IX2154.2
216000 WRITE-TEST-GF-03-END.                                            IX2154.2
216100     CLOSE   IX-FD3.                                              IX2154.2
216200 READ-INIT-F1-O3.                                                 IX2154.2
216300     OPEN    INPUT  IX-FD3.                                       IX2154.2
216400     MOVE    "READ-TEST-F1-O3" TO PAR-NAME.                       IX2154.2
216500     MOVE    "READ FILE IX-FD3" TO FEATURE.                       IX2154.2
216600     MOVE    ZERO TO EXCUT-COUNTER-06V00.                         IX2154.2
216700     MOVE    02   TO RECKEY-NUM.                                  IX2154.2
216800     MOVE    002 TO ALTKEY1-NUM.                                  IX2154.2
216900 READ-TEST-F1-O3-R1.                                              IX2154.2
217000     READ    IX-FD3 NEXT RECORD AT END  GO TO READ-TEST-F1-O3.    IX2154.2
217100     MOVE    IX-FD3-RECKEY-AREA TO FDW-RECKEY-1-13.               IX2154.2
217200     MOVE    IX-FD3-ALTKEY1-AREA TO FDW-ALTKEY1-1-20.             IX2154.2
217300     IF      FDW-RECKEY-11-13 EQUAL TO RECKEY-NUM                 IX2154.2
217400             AND FDW-ALTKEY1-11-13 EQUAL TO ALTKEY1-NUM           IX2154.2
217500             NEXT SENTENCE                                        IX2154.2
217600             ELSE                                                 IX2154.2
217700             PERFORM  READ-FAIL-F1-O3.                            IX2154.2
217800     IF      EXCUT-COUNTER-06V00 NOT LESS THAN 200                IX2154.2
217900             GO TO READ-TEST-F1-O3.                               IX2154.2
218000     ADD     001  TO EXCUT-COUNTER-06V00.                         IX2154.2
218100     ADD     002  TO RECKEY-NUM                                   IX2154.2
218200     ADD     002  TO ALTKEY1-NUM.                                 IX2154.2
218300     GO TO   READ-TEST-F1-O3-R1.                                  IX2154.2
218400 READ-TEST-F1-O3.                                                 IX2154.2
218500     IF      FAIL-SW EQUAL TO 1 GO TO READ-EXIT-F1-O3.            IX2154.2
218600     IF      EXCUT-COUNTER-06V00 EQUAL TO 200                     IX2154.2
218700             PERFORM PASS                                         IX2154.2
218800             MOVE "200 RECORDS VERIFIED" TO RE-MARK               IX2154.2
218900     ELSE    PERFORM FAIL                                         IX2154.2
219000             MOVE                                                 IX2154.2
219100          "IX-28 OR IX-41; INCORRECT NUMBER OF RECORDS" TO RE-MARKIX2154.2
219200             MOVE 200 TO CORRECT-18V0                             IX2154.2
219300             MOVE EXCUT-COUNTER-06V00 TO COMPUTED-18V0.           IX2154.2
219400     PERFORM PRINT-DETAIL.                                        IX2154.2
219500     GO TO   READ-EXIT-F1-O3.                                     IX2154.2
219600 READ-FAIL-F1-O3.                                                 IX2154.2
219700     MOVE 1 TO FAIL-SW.                                           IX2154.2
219800     PERFORM FAIL.                                                IX2154.2
219900     MOVE    FDW-RECKEY-11-13  TO COMPUTED-18V0.                  IX2154.2
220000     MOVE    RECKEY-NUM  TO CORRECT-18V0.                         IX2154.2
220100     ADD     01 TO REC-CT.                                        IX2154.2
220200     MOVE    "NUM EMBEDDED IN RECKEY"  TO RE-MARK.                IX2154.2
220300     PERFORM PRINT-DETAIL.                                        IX2154.2
220400 READ-EXIT-F1-O3.                                                 IX2154.2
220500     CLOSE   IX-FD3.                                              IX2154.2
220600 START-INIT-GF-19.                                                IX2154.2
220700     OPEN    I-O  IX-FD3.                                         IX2154.2
220800     MOVE "----------" TO DUMMY-RECORD. PERFORM BLANK-LINE-PRINT. IX2154.2
220900     MOVE "START-TEST-GF-19" TO PAR-NAME.                         IX2154.2
221000     MOVE    "START QUAL REC-KEY" TO FEATURE.                     IX2154.2
221100     MOVE    "FFGGGGGGGG096" TO IX-FD3-RECKEY-AREA.               IX2154.2
221200 START-TEST-GF-19.                                                IX2154.2
221300     DELETE  IX-FD3 INVALID KEY                                   IX2154.2
221400     MOVE "IX-21; INVALID KEY "         TO COMPUTED-A             IX2154.2
221500             GO TO START-FAIL-GF-19.                              IX2154.2
221600     PERFORM START-INIT-FD3 THRU START-INIT-FD3-EXIT.             IX2154.2
221700     IF      INIT-FLAG NOT EQUAL ZERO                             IX2154.2
221800             GO TO START-DELETE-GF-19.                            IX2154.2
221900     MOVE    "FFGGGGGGGG096" TO IX-FD3-RECKEY-AREA.               IX2154.2
222000     START IX-FD3 KEY IS EQUAL TO IX-FD3-KEY IN IX-FD3-RECKEY-AREAIX2154.2
222100             INVALID KEY  PERFORM PASS                            IX2154.2
222200     MOVE "OK.;    INVALID KEY ON START OF DELETED RECORD"        IX2154.2
222300                  TO RE-MARK                                      IX2154.2
222400             GO TO START-WRITE-GF-19.                             IX2154.2
222500     READ    IX-FD3 NEXT RECORD  AT END                           IX2154.2
222600             MOVE "AT END ON READ" TO COMPUTED-A                  IX2154.2
222700             GO TO START-FAIL-GF-19.                              IX2154.2
222800     MOVE    IX-FD3-240        TO  FILE-RECORD-INFO (1).          IX2154.2
222900     MOVE     XRECORD-NUMBER (1)  TO COMPUTED-18V0.               IX2154.2
223000 START-FAIL-GF-19.                                                IX2154.2
223100     PERFORM FAIL.                                                IX2154.2
223200     MOVE    "INVALID KEY ON START" TO CORRECT-A.                 IX2154.2
223300     MOVE "WRONG RECORD NUMBER FOUND; IX-28 OR IX-36" TO RE-MARK. IX2154.2
223400     GO TO   START-WRITE-GF-19.                                   IX2154.2
223500 START-DELETE-GF-19.                                              IX2154.2
223600     PERFORM DE-LETE.                                             IX2154.2
223700 START-WRITE-GF-19.                                               IX2154.2
223800     PERFORM PRINT-DETAIL.                                        IX2154.2
223900 START-INIT-GF-20.                                                IX2154.2
224000     MOVE "----------" TO DUMMY-RECORD. PERFORM BLANK-LINE-PRINT. IX2154.2
224100     MOVE "START-TEST-GF-20" TO PAR-NAME.                         IX2154.2
224200     MOVE    "CCCCCCCCCC020" TO IX-FD3-RECKEY-AREA.               IX2154.2
224300 START-TEST-GF-20.                                                IX2154.2
224400     DELETE  IX-FD3 INVALID KEY                                   IX2154.2
224500             MOVE  "INVALID KEY (DELETE)" TO COMPUTED-A           IX2154.2
224600             GO TO START-FAIL-GF-20.                              IX2154.2
224700     PERFORM START-INIT-FD3 THRU START-INIT-FD3-EXIT.             IX2154.2
224800     IF      INIT-FLAG NOT EQUAL ZERO                             IX2154.2
224900             GO TO START-DELETE-GF-20.                            IX2154.2
225000     MOVE    "CCCCCCCCCC020" TO IX-FD3-RECKEY-AREA.               IX2154.2
225100     START   IX-FD3 KEY IS EQUAL TO IX-FD3-KEY                    IX2154.2
225200             OF IX-FD3-RECKEY-AREA                                IX2154.2
225300             INVALID KEY  PERFORM PASS                            IX2154.2
225400      MOVE "OK.;   INVALID KEY ON START OF DELETED RECORD"        IX2154.2
225500                  TO RE-MARK                                      IX2154.2
225600             GO TO START-WRITE-GF-20.                             IX2154.2
225700     READ    IX-FD3 NEXT RECORD  AT END                           IX2154.2
225800             MOVE "AT END ON READ" TO COMPUTED-A                  IX2154.2
225900             GO TO START-FAIL-GF-20.                              IX2154.2
226000     MOVE    IX-FD3-240        TO  FILE-RECORD-INFO (1).          IX2154.2
226100     MOVE     XRECORD-NUMBER (1)  TO COMPUTED-18V0.               IX2154.2
226200 START-FAIL-GF-20.                                                IX2154.2
226300     PERFORM FAIL.                                                IX2154.2
226400     MOVE    "INVALID KEY ON START" TO CORRECT-A.                 IX2154.2
226500     MOVE "WRONG RECORD NUMBER FOUND; IX-28 OR IX-36" TO RE-MARK. IX2154.2
226600     GO TO   START-WRITE-GF-20.                                   IX2154.2
226700 START-DELETE-GF-20.                                              IX2154.2
226800     PERFORM DE-LETE.                                             IX2154.2
226900 START-WRITE-GF-20.                                               IX2154.2
227000     PERFORM PRINT-DETAIL.                                        IX2154.2
227100 START-INIT-GF-21.                                                IX2154.2
227200     MOVE "----------" TO DUMMY-RECORD. PERFORM BLANK-LINE-PRINT. IX2154.2
227300     MOVE "START-TEST-GF-21" TO PAR-NAME.                         IX2154.2
227400     MOVE    "SSSSSSSSST362" TO IX-FD3-RECKEY-AREA.               IX2154.2
227500 START-TEST-GF-21.                                                IX2154.2
227600     DELETE  IX-FD3 INVALID KEY                                   IX2154.2
227700             MOVE  "INVALID KEY (DELETE)" TO COMPUTED-A           IX2154.2
227800             GO TO START-FAIL-GF-21.                              IX2154.2
227900     PERFORM START-INIT-FD3 THRU START-INIT-FD3-EXIT.             IX2154.2
228000     IF      INIT-FLAG NOT EQUAL ZERO                             IX2154.2
228100             GO TO START-DELETE-GF-21.                            IX2154.2
228200     MOVE    "SSSSSSSSST362" TO IX-FD3-RECKEY-AREA.               IX2154.2
228300     START   IX-FD3 KEY IS EQUAL TO                               IX2154.2
228400             IX-FD3-KEY IN                                        IX2154.2
228500             IX-FD3-RECKEY-AREA                                   IX2154.2
228600             INVALID KEY  PERFORM PASS                            IX2154.2
228700     MOVE "OK.;    INVALID KEY ON START OF DELETED RECORD"        IX2154.2
228800                  TO RE-MARK                                      IX2154.2
228900             GO TO START-WRITE-GF-21.                             IX2154.2
229000     READ    IX-FD3 NEXT RECORD  AT END                           IX2154.2
229100             MOVE "AT END ON READ" TO COMPUTED-A                  IX2154.2
229200             GO TO START-FAIL-GF-21.                              IX2154.2
229300     MOVE    IX-FD3-240        TO  FILE-RECORD-INFO (1).          IX2154.2
229400     MOVE     XRECORD-NUMBER (1)  TO COMPUTED-18V0.               IX2154.2
229500 START-FAIL-GF-21.                                                IX2154.2
229600     PERFORM FAIL.                                                IX2154.2
229700     MOVE    "INVALID KEY ON START" TO CORRECT-A.                 IX2154.2
229800     MOVE "WRONG RECORD NUMBER FOUND; IX-28 OR IX-36" TO RE-MARK. IX2154.2
229900     GO TO   START-WRITE-GF-21.                                   IX2154.2
230000 START-DELETE-GF-21.                                              IX2154.2
230100     PERFORM DE-LETE.                                             IX2154.2
230200 START-WRITE-GF-21.                                               IX2154.2
230300     PERFORM PRINT-DETAIL.                                        IX2154.2
230400 START-INIT-GF-22.                                                IX2154.2
230500     MOVE    "START QUAL ALT-KEY-1" TO FEATURE.                   IX2154.2
230600     MOVE "----------" TO DUMMY-RECORD. PERFORM BLANK-LINE-PRINT. IX2154.2
230700     MOVE "START-TEST-GF-22" TO PAR-NAME.                         IX2154.2
230800     MOVE    "EEEEEEEEFF064"  TO IX-FD3-RECKEY-AREA.              IX2154.2
230900 START-TEST-GF-22.                                                IX2154.2
231000     READ    IX-FD3                                               IX2154.2
231100             INVALID KEY  PERFORM FAIL                            IX2154.2
231200     MOVE "IX-28;F2 INVALID KEY PATH TAKEN ON INITIAL READ"       IX2154.2
231300                   TO RE-MARK                                     IX2154.2
231400             MOVE  "INVALID KEY ON READ" TO COMPUTED-A            IX2154.2
231500             GO TO START-FAIL-GF-22.                              IX2154.2
231600     MOVE    IX-FD3-240       TO FILE-RECORD-INFO (3).            IX2154.2
231700     IF      XRECORD-NUMBER (3) NOT EQUAL TO 32                   IX2154.2
231800             MOVE 32 TO RECNO                                     IX2154.2
231900             PERFORM DISPLAY-RECORD-KEYS                          IX2154.2
232000     MOVE "IX-28;F2 WRONG RECORD FOUND ON INITIAL READ" TO RE-MARKIX2154.2
232100             MOVE  XRECORD-NUMBER (3) TO COMPUTED-18V0            IX2154.2
232200             GO TO START-FAIL-GF-22.                              IX2154.2
232300     PERFORM START-INIT-FD3 THRU START-INIT-FD3-EXIT.             IX2154.2
232400     IF      INIT-FLAG NOT EQUAL ZERO                             IX2154.2
232500             GO TO START-DELETE-GF-22.                            IX2154.2
232600     MOVE    "EEEEEEEEEE000ALTKEY1" TO ALTERNATE-KEY1 (3).        IX2154.2
232700     MOVE    "WWWWWWWWWW400ALTKEY2" TO ALTERNATE-KEY2 (3).        IX2154.2
232800     MOVE    FILE-RECORD-INFO (3) TO IX-FD3R1-F-G-242.            IX2154.2
232900     REWRITE IX-FD3R1-F-G-242 INVALID KEY                         IX2154.2
233000     MOVE "IX-33;   INVALID KEY PATH TAKEN ON REWRITE" TO RE-MARK IX2154.2
233100             MOVE  "INVALID KEY; REWRITE" TO COMPUTED-A           IX2154.2
233200             GO TO START-FAIL-GF-22.                              IX2154.2
233300     PERFORM START-INIT-FD3 THRU START-INIT-FD3-EXIT.             IX2154.2
233400     IF      INIT-FLAG NOT EQUAL ZERO                             IX2154.2
233500             GO TO START-FAIL-GF-22.                              IX2154.2
233600     MOVE    "EEEEEEEEEE000ALTKEY1"  TO IX-FD3-ALTKEY1-AREA.      IX2154.2
233700     START   IX-FD3                                               IX2154.2
233800             KEY IS EQUAL TO IX-FD3-KEY OF                        IX2154.2
233900             IX-FD3-ALTKEY1-AREA                                  IX2154.2
234000             INVALID KEY                                          IX2154.2
234100     MOVE "IX-36;  INVALID KEY ON START OF REWRITTEN RECORD"      IX2154.2
234200             TO RE-MARK                                           IX2154.2
234300             MOVE  "INVALID KEY ON START" TO COMPUTED-A           IX2154.2
234400             GO TO START-FAIL-GF-22.                              IX2154.2
234500     READ    IX-FD3 NEXT RECORD AT END                            IX2154.2
234600     MOVE "IX-28;F1 AT END ON READ AFTER START" TO RE-MARK        IX2154.2
234700             MOVE "AT END ON READ" TO COMPUTED-A                  IX2154.2
234800             GO TO START-FAIL-GF-22.                              IX2154.2
234900     MOVE    IX-FD3-240        TO FILE-RECORD-INFO (1).           IX2154.2
235000     IF      XRECORD-NUMBER (1) EQUAL TO 32                       IX2154.2
235100             PERFORM PASS                                         IX2154.2
235200             GO TO START-WRITE-GF-22.                             IX2154.2
235300     PERFORM FAIL.                                                IX2154.2
235400     MOVE    ALTERNATE-KEY1 (1)  TO WRK-FDW-ALTKEY1.              IX2154.2
235500     MOVE    FDW-ALTKEY1-1-20  TO COMPUTED-A.                     IX2154.2
235600     MOVE    "EEEEEEEEEE000ALTKEY1"  TO CORRECT-A.                IX2154.2
235700     MOVE "IX-28 OR IX-36; INCORR ALTERN RECORD KEY1" TO RE-MARK. IX2154.2
235800     PERFORM PRINT-DETAIL.                                        IX2154.2
235900     MOVE   "WRONG RECORD NUMBER FOUND ON READ ALTERNATE REC KEY1"IX2154.2
236000             TO RE-MARK.                                          IX2154.2
236100     MOVE    XRECORD-NUMBER (1) TO COMPUTED-18V0.                 IX2154.2
236200 START-FAIL-GF-22.                                                IX2154.2
236300     PERFORM FAIL.                                                IX2154.2
236400     MOVE    32  TO CORRECT-18V0.                                 IX2154.2
236500     GO TO   START-WRITE-GF-22.                                   IX2154.2
236600 START-DELETE-GF-22.                                              IX2154.2
236700     PERFORM DE-LETE.                                             IX2154.2
236800 START-WRITE-GF-22.                                               IX2154.2
236900     PERFORM PRINT-DETAIL.                                        IX2154.2
237000 START-INIT-GF-23.                                                IX2154.2
237100     MOVE "----------" TO DUMMY-RECORD. PERFORM BLANK-LINE-PRINT. IX2154.2
237200     MOVE "START-TEST-GF-23" TO PAR-NAME.                         IX2154.2
237300     MOVE    "BBBBBBBCCC006"  TO IX-FD3-RECKEY-AREA.              IX2154.2
237400 START-TEST-GF-23.                                                IX2154.2
237500     READ    IX-FD3                                               IX2154.2
237600             INVALID KEY                                          IX2154.2
237700     MOVE "IX-28;F2 INVALID KEY PATH TAKEN ON INITIAL READ"       IX2154.2
237800                   TO RE-MARK                                     IX2154.2
237900             MOVE  "INVALID KEY ON READ" TO COMPUTED-A            IX2154.2
238000             GO TO START-FAIL-GF-23.                              IX2154.2
238100     MOVE    IX-FD3-240       TO FILE-RECORD-INFO (3).            IX2154.2
238200     IF      XRECORD-NUMBER (3) NOT EQUAL TO 3                    IX2154.2
238300             MOVE 3 TO RECNO                                      IX2154.2
238400             PERFORM DISPLAY-RECORD-KEYS                          IX2154.2
238500     MOVE "IX-28;F2 WRONG RECORD FOUND ON INITIAL READ" TO RE-MARKIX2154.2
238600             MOVE  XRECORD-NUMBER (3) TO COMPUTED-18V0            IX2154.2
238700             GO TO START-FAIL-GF-23.                              IX2154.2
238800     PERFORM START-INIT-FD3 THRU START-INIT-FD3-EXIT.             IX2154.2
238900     IF      INIT-FLAG NOT EQUAL ZERO                             IX2154.2
239000             GO TO START-DELETE-GF-23.                            IX2154.2
239100     MOVE    "AAAAAAAAAA400ALTKEY1" TO ALTERNATE-KEY1 (3).        IX2154.2
239200     MOVE    "ZZZZZZZZZZ002ALTKEY2" TO ALTERNATE-KEY2 (3).        IX2154.2
239300     MOVE    FILE-RECORD-INFO (3) TO IX-FD3R1-F-G-242.            IX2154.2
239400     REWRITE IX-FD3R1-F-G-242 INVALID KEY                         IX2154.2
239500     MOVE "IX-33;   INVALID KEY PATH TAKEN ON REWRITE" TO RE-MARK IX2154.2
239600             MOVE  "INVALID KEY; REWRITE" TO COMPUTED-A           IX2154.2
239700             GO TO START-FAIL-GF-23.                              IX2154.2
239800     PERFORM START-INIT-FD3 THRU START-INIT-FD3-EXIT.             IX2154.2
239900     IF      INIT-FLAG NOT EQUAL ZERO                             IX2154.2
240000             GO TO START-FAIL-GF-23.                              IX2154.2
240100     MOVE    "AAAAAAAAAA400ALTKEY1"  TO IX-FD3-ALTKEY1-AREA.      IX2154.2
240200     START   IX-FD3                                               IX2154.2
240300                    KEY IS EQUAL TO                               IX2154.2
240400             IX-FD3-KEY                                           IX2154.2
240500             IN IX-FD3-ALTKEY1-AREA                               IX2154.2
240600             INVALID KEY                                          IX2154.2
240700     MOVE "IX-36;  INVALID KEY ON START OF REWRITTEN RECORD"      IX2154.2
240800             TO RE-MARK                                           IX2154.2
240900             MOVE  "INVALID KEY ON START" TO COMPUTED-A           IX2154.2
241000             GO TO START-FAIL-GF-23.                              IX2154.2
241100     READ    IX-FD3 NEXT RECORD AT END                            IX2154.2
241200     MOVE "IX-28;F1 AT END ON READ AFTER START" TO RE-MARK        IX2154.2
241300             MOVE "AT END ON READ" TO COMPUTED-A                  IX2154.2
241400             GO TO START-FAIL-GF-23.                              IX2154.2
241500     MOVE    IX-FD3-240        TO FILE-RECORD-INFO (1).           IX2154.2
241600     IF      XRECORD-NUMBER (1) EQUAL TO 3                        IX2154.2
241700             PERFORM PASS                                         IX2154.2
241800             GO TO START-WRITE-GF-23.                             IX2154.2
241900     MOVE    ALTERNATE-KEY1 (1)  TO WRK-FDW-ALTKEY1.              IX2154.2
242000     MOVE    FDW-ALTKEY1-1-20  TO COMPUTED-A.                     IX2154.2
242100     MOVE    "AAAAAAAAAA400ALTKEY1"  TO CORRECT-A.                IX2154.2
242200     MOVE "IX-28 OR IX-36; INCORR ALTERN RECORD KEY1" TO RE-MARK. IX2154.2
242300     PERFORM PRINT-DETAIL.                                        IX2154.2
242400     MOVE   "WRONG RECORD NUMBER FOUND ON READ ALTERNATE REC KEY1"IX2154.2
242500             TO RE-MARK.                                          IX2154.2
242600     MOVE    XRECORD-NUMBER (1) TO COMPUTED-18V0.                 IX2154.2
242700 START-FAIL-GF-23.                                                IX2154.2
242800     PERFORM FAIL.                                                IX2154.2
242900     MOVE 3 TO CORRECT-18V0.                                      IX2154.2
243000     GO TO   START-WRITE-GF-23.                                   IX2154.2
243100 START-DELETE-GF-23.                                              IX2154.2
243200     PERFORM DE-LETE.                                             IX2154.2
243300 START-WRITE-GF-23.                                               IX2154.2
243400     PERFORM PRINT-DETAIL.                                        IX2154.2
243500 START-INIT-GF-24.                                                IX2154.2
243600     MOVE "----------" TO DUMMY-RECORD. PERFORM BLANK-LINE-PRINT. IX2154.2
243700     MOVE "START-TEST-GF-24" TO PAR-NAME.                         IX2154.2
243800     MOVE    "SSSSSSSSSS360"  TO IX-FD3-RECKEY-AREA.              IX2154.2
243900 START-TEST-GF-24.                                                IX2154.2
244000     READ    IX-FD3                                               IX2154.2
244100             INVALID KEY                                          IX2154.2
244200     MOVE "IX-28;F2 INVALID KEY PATH TAKEN ON INITIAL READ"       IX2154.2
244300                   TO RE-MARK                                     IX2154.2
244400             MOVE  "INVALID KEY ON READ" TO COMPUTED-A            IX2154.2
244500             GO TO START-FAIL-GF-24.                              IX2154.2
244600     MOVE    IX-FD3-240       TO FILE-RECORD-INFO (3).            IX2154.2
244700     IF      XRECORD-NUMBER (3) NOT EQUAL TO 180                  IX2154.2
244800             MOVE 55 TO RECNO                                     IX2154.2
244900             PERFORM DISPLAY-RECORD-KEYS                          IX2154.2
245000     MOVE "IX-28;F2 WRONG RECORD FOUND ON INITIAL READ" TO RE-MARKIX2154.2
245100             MOVE  XRECORD-NUMBER (3) TO COMPUTED-18V0            IX2154.2
245200             GO TO START-FAIL-GF-24.                              IX2154.2
245300     IF      INIT-FLAG NOT EQUAL ZERO                             IX2154.2
245400             GO TO START-DELETE-GF-24.                            IX2154.2
245500     MOVE    "AAGGGGGGGG100ALTKEY1" TO ALTERNATE-KEY1 (3).        IX2154.2
245600     MOVE    "GGGGGGGGGG100ALTKEY2" TO ALTERNATE-KEY2 (3).        IX2154.2
245700     MOVE    FILE-RECORD-INFO (3) TO IX-FD3R1-F-G-242.            IX2154.2
245800     REWRITE IX-FD3R1-F-G-242 INVALID KEY                         IX2154.2
245900     MOVE "IX-33;   INVALID KEY PATH TAKEN ON REWRITE" TO RE-MARK IX2154.2
246000             MOVE  "INVALID KEY; REWRITE" TO COMPUTED-A           IX2154.2
246100             GO TO START-FAIL-GF-24.                              IX2154.2
246200     PERFORM START-INIT-FD3 THRU START-INIT-FD3-EXIT.             IX2154.2
246300     IF      INIT-FLAG NOT EQUAL ZERO                             IX2154.2
246400             GO TO START-FAIL-GF-24.                              IX2154.2
246500     MOVE    "AAGGGGGGGG100ALTKEY1"  TO IX-FD3-ALTKEY1-AREA.      IX2154.2
246600     START   IX-FD3                                               IX2154.2
246700             KEY IS EQUAL TO                                      IX2154.2
246800             IX-FD3-KEY                                           IX2154.2
246900             OF IX-FD3-ALTKEY1-AREA                               IX2154.2
247000             INVALID KEY  PERFORM FAIL                            IX2154.2
247100     MOVE "IX-36;  INVALID KEY ON START OF REWRITTEN RECORD"      IX2154.2
247200             TO RE-MARK                                           IX2154.2
247300             MOVE  "INVALID KEY ON START" TO COMPUTED-A           IX2154.2
247400             GO TO START-FAIL-GF-24.                              IX2154.2
247500     READ    IX-FD3 NEXT RECORD AT END                            IX2154.2
247600     MOVE "IX-28;F1 AT END ON READ AFTER START" TO RE-MARK        IX2154.2
247700             MOVE "AT END ON READ" TO COMPUTED-A                  IX2154.2
247800             GO TO START-FAIL-GF-24.                              IX2154.2
247900     MOVE    IX-FD3-240        TO FILE-RECORD-INFO (1).           IX2154.2
248000     IF      XRECORD-NUMBER (1) EQUAL TO 180                      IX2154.2
248100             PERFORM PASS                                         IX2154.2
248200             GO TO START-WRITE-GF-24.                             IX2154.2
248300     MOVE    ALTERNATE-KEY1 (1)  TO WRK-FDW-ALTKEY1.              IX2154.2
248400     MOVE    FDW-ALTKEY1-1-20  TO COMPUTED-A.                     IX2154.2
248500     MOVE    "AAGGGGGGGG100ALTKEY1"  TO CORRECT-A.                IX2154.2
248600     MOVE "IX-28 OR IX-36; INCORR ALTERN RECORD KEY1" TO RE-MARK. IX2154.2
248700     PERFORM PRINT-DETAIL.                                        IX2154.2
248800     MOVE   "WRONG RECORD NUMBER FOUND ON READ ALTERNATE REC KEY1"IX2154.2
248900             TO RE-MARK.                                          IX2154.2
249000     MOVE    XRECORD-NUMBER (1) TO COMPUTED-18V0.                 IX2154.2
249100 START-FAIL-GF-24.                                                IX2154.2
249200     PERFORM FAIL.                                                IX2154.2
249300     MOVE    180  TO CORRECT-18V0.                                IX2154.2
249400     GO TO   START-WRITE-GF-24.                                   IX2154.2
249500 START-DELETE-GF-24.                                              IX2154.2
249600     PERFORM DE-LETE.                                             IX2154.2
249700 START-WRITE-GF-24.                                               IX2154.2
249800     PERFORM PRINT-DETAIL.                                        IX2154.2
249900 START-INIT-GF-25.                                                IX2154.2
250000     MOVE    "START QUAL ALT-KEY-2" TO FEATURE.                   IX2154.2
250100     MOVE "----------" TO DUMMY-RECORD. PERFORM BLANK-LINE-PRINT. IX2154.2
250200     MOVE "START-TEST-GF-25" TO PAR-NAME.                         IX2154.2
250300     MOVE    "CCCCCDDDDD030"  TO IX-FD3-RECKEY-AREA.              IX2154.2
250400 START-TEST-GF-25.                                                IX2154.2
250500     READ    IX-FD3                                               IX2154.2
250600             INVALID KEY                                          IX2154.2
250700     MOVE "IX-28;F2 INVALID KEY PATH TAKEN ON INITIAL READ"       IX2154.2
250800                   TO RE-MARK                                     IX2154.2
250900             MOVE  "INVALID KEY ON READ" TO COMPUTED-A            IX2154.2
251000             GO TO START-FAIL-GF-25.                              IX2154.2
251100     MOVE    IX-FD3-240       TO FILE-RECORD-INFO (3).            IX2154.2
251200     IF      XRECORD-NUMBER (3) NOT EQUAL TO 15                   IX2154.2
251300             MOVE 15 TO RECNO                                     IX2154.2
251400             PERFORM DISPLAY-RECORD-KEYS                          IX2154.2
251500     MOVE "IX-28;F2 WRONG RECORD FOUND ON INITIAL READ" TO RE-MARKIX2154.2
251600             MOVE  XRECORD-NUMBER (3) TO COMPUTED-18V0            IX2154.2
251700             GO TO START-FAIL-GF-25.                              IX2154.2
251800     PERFORM START-INIT-FD3 THRU START-INIT-FD3-EXIT.             IX2154.2
251900     IF      INIT-FLAG NOT EQUAL ZERO                             IX2154.2
252000             GO TO START-DELETE-GF-25.                            IX2154.2
252100     MOVE    "BCBCBCBCBC200ALTKEY1" TO ALTERNATE-KEY1 (3).        IX2154.2
252200     MOVE    "CBCBCBCBCB100ALTKEY2" TO ALTERNATE-KEY2 (3).        IX2154.2
252300     MOVE    FILE-RECORD-INFO (3) TO IX-FD3R1-F-G-242.            IX2154.2
252400     REWRITE IX-FD3R1-F-G-242 INVALID KEY                         IX2154.2
252500      MOVE "IX-33;  INVALID KEY PATH TAKEN ON REWRITE" TO RE-MARK IX2154.2
252600             MOVE  "INVALID KEY; REWRITE" TO COMPUTED-A           IX2154.2
252700             GO TO START-FAIL-GF-25.                              IX2154.2
252800     PERFORM START-INIT-FD3 THRU START-INIT-FD3-EXIT.             IX2154.2
252900     IF      INIT-FLAG NOT EQUAL ZERO                             IX2154.2
253000             GO TO START-FAIL-GF-25.                              IX2154.2
253100     MOVE    "CBCBCBCBCB100ALTKEY2"  TO IX-FD3-ALTKEY2-AREA.      IX2154.2
253200     START   IX-FD3                                               IX2154.2
253300                    KEY IS EQUAL TO                               IX2154.2
253400             IX-FD3-KEY                                           IX2154.2
253500             IN                                                   IX2154.2
253600             IX-FD3-ALTKEY2-AREA                                  IX2154.2
253700             INVALID KEY                                          IX2154.2
253800     MOVE "IX-36;  INVALID KEY ON START OF REWRITTEN RECORD"      IX2154.2
253900             TO RE-MARK                                           IX2154.2
254000             MOVE  "INVALID KEY ON START" TO COMPUTED-A           IX2154.2
254100             GO TO START-FAIL-GF-25.                              IX2154.2
254200     READ    IX-FD3 NEXT RECORD AT END                            IX2154.2
254300     MOVE "IX-28;F1 AT END ON READ AFTER START" TO RE-MARK        IX2154.2
254400             MOVE "AT END ON READ" TO COMPUTED-A                  IX2154.2
254500             GO TO START-FAIL-GF-25.                              IX2154.2
254600     MOVE    IX-FD3-240        TO FILE-RECORD-INFO (1).           IX2154.2
254700     IF      XRECORD-NUMBER (1) EQUAL TO 15                       IX2154.2
254800             PERFORM PASS                                         IX2154.2
254900             GO TO START-WRITE-GF-25.                             IX2154.2
255000     MOVE    ALTERNATE-KEY2 (1)  TO WRK-FDW-ALTKEY2.              IX2154.2
255100     MOVE    FDW-ALTKEY2-1-20  TO COMPUTED-A.                     IX2154.2
255200     MOVE    "CBCBCBCBCB100ALTKEY2"  TO CORRECT-A.                IX2154.2
255300     MOVE "IX-28 OR IX-36; INCORR ALTERN RECORD KEY2" TO RE-MARK. IX2154.2
255400     PERFORM PRINT-DETAIL.                                        IX2154.2
255500     MOVE   "WRONG RECORD NUMBER FOUND ON READ ALTERNATE REC KEY2"IX2154.2
255600             TO RE-MARK.                                          IX2154.2
255700     MOVE    XRECORD-NUMBER (1) TO COMPUTED-18V0.                 IX2154.2
255800 START-FAIL-GF-25.                                                IX2154.2
255900     PERFORM FAIL.                                                IX2154.2
256000     MOVE    15  TO CORRECT-18V0.                                 IX2154.2
256100     GO TO   START-WRITE-GF-25.                                   IX2154.2
256200 START-DELETE-GF-25.                                              IX2154.2
256300     PERFORM DE-LETE.                                             IX2154.2
256400 START-WRITE-GF-25.                                               IX2154.2
256500     PERFORM PRINT-DETAIL.                                        IX2154.2
256600 START-INIT-GF-26.                                                IX2154.2
256700     MOVE "----------" TO DUMMY-RECORD. PERFORM BLANK-LINE-PRINT. IX2154.2
256800     MOVE "START-TEST-GF-26" TO PAR-NAME.                         IX2154.2
256900     MOVE    "TTTTTTTTTT380"  TO IX-FD3-RECKEY-AREA.              IX2154.2
257000 START-TEST-GF-26.                                                IX2154.2
257100     READ    IX-FD3                                               IX2154.2
257200             INVALID KEY                                          IX2154.2
257300     MOVE "IX-28;F2 INVALID KEY PATH TAKEN ON INITIAL READ"       IX2154.2
257400                   TO RE-MARK                                     IX2154.2
257500             MOVE  "INVALID KEY ON READ" TO COMPUTED-A            IX2154.2
257600             GO TO START-FAIL-GF-26.                              IX2154.2
257700     MOVE    IX-FD3-240       TO FILE-RECORD-INFO (3).            IX2154.2
257800     IF      XRECORD-NUMBER (3) NOT EQUAL TO 190                  IX2154.2
257900             MOVE 65 TO RECNO                                     IX2154.2
258000             PERFORM DISPLAY-RECORD-KEYS                          IX2154.2
258100     MOVE "IX-28;F2 WRONG RECORD FOUND ON INITIAL READ" TO RE-MARKIX2154.2
258200             MOVE  XRECORD-NUMBER (3) TO COMPUTED-18V0            IX2154.2
258300             GO TO START-FAIL-GF-26.                              IX2154.2
258400     PERFORM START-INIT-FD3 THRU START-INIT-FD3-EXIT.             IX2154.2
258500     IF      INIT-FLAG NOT EQUAL ZERO                             IX2154.2
258600             GO TO START-DELETE-GF-26.                            IX2154.2
258700     MOVE    "DCDCDCDCDC100ALTKEY1" TO ALTERNATE-KEY1 (3).        IX2154.2
258800     MOVE    "DCDCDCDCDC100ALTKEY2" TO ALTERNATE-KEY2 (3).        IX2154.2
258900     MOVE    FILE-RECORD-INFO (3) TO IX-FD3R1-F-G-242.            IX2154.2
259000     REWRITE IX-FD3R1-F-G-242 INVALID KEY                         IX2154.2
259100      MOVE "IX-33;  INVALID KEY PATH TAKEN ON REWRITE" TO RE-MARK IX2154.2
259200             MOVE  "INVALID KEY; REWRITE" TO COMPUTED-A           IX2154.2
259300             GO TO START-FAIL-GF-26.                              IX2154.2
259400     PERFORM START-INIT-FD3 THRU START-INIT-FD3-EXIT.             IX2154.2
259500     IF      INIT-FLAG NOT EQUAL ZERO                             IX2154.2
259600             GO TO START-FAIL-GF-26.                              IX2154.2
259700     MOVE    "DCDCDCDCDC100ALTKEY2"  TO IX-FD3-ALTKEY2-AREA.      IX2154.2
259800     START   IX-FD3                                               IX2154.2
259900             KEY IS EQUAL TO                                      IX2154.2
260000             IX-FD3-KEY OF IX-FD3-ALTKEY2-AREA                    IX2154.2
260100             INVALID KEY                                          IX2154.2
260200      MOVE "IX-36; INVALID KEY ON START OF REWRITTEN RECORD"      IX2154.2
260300             TO RE-MARK                                           IX2154.2
260400             MOVE  "INVALID KEY ON START" TO COMPUTED-A           IX2154.2
260500             GO TO START-FAIL-GF-26.                              IX2154.2
260600     READ    IX-FD3 NEXT RECORD AT END                            IX2154.2
260700     MOVE "IX-28;F1 AT END ON READ AFTER START" TO RE-MARK        IX2154.2
260800             MOVE "AT END ON READ" TO COMPUTED-A                  IX2154.2
260900             GO TO START-FAIL-GF-26.                              IX2154.2
261000     MOVE    IX-FD3-240        TO FILE-RECORD-INFO (1).           IX2154.2
261100     IF      XRECORD-NUMBER (1) EQUAL TO 190                      IX2154.2
261200             PERFORM PASS                                         IX2154.2
261300             GO TO START-WRITE-GF-26.                             IX2154.2
261400     MOVE    ALTERNATE-KEY2 (1)  TO WRK-FDW-ALTKEY2.              IX2154.2
261500     MOVE    FDW-ALTKEY2-1-20  TO COMPUTED-A.                     IX2154.2
261600     MOVE    "DCDCDCDCDC100ALTKEY2"  TO CORRECT-A.                IX2154.2
261700     MOVE "IX-28 OR IX-36; INCORR ALTERN RECORD KEY2" TO RE-MARK. IX2154.2
261800     PERFORM PRINT-DETAIL.                                        IX2154.2
261900     MOVE   "WRONG RECORD NUMBER FOUND ON READ ALTERNATE REC KEY2"IX2154.2
262000             TO RE-MARK.                                          IX2154.2
262100     MOVE    XRECORD-NUMBER (1) TO COMPUTED-18V0.                 IX2154.2
262200 START-FAIL-GF-26.                                                IX2154.2
262300     PERFORM FAIL.                                                IX2154.2
262400     MOVE    190  TO CORRECT-18V0.                                IX2154.2
262500     GO TO   START-WRITE-GF-26.                                   IX2154.2
262600 START-DELETE-GF-26.                                              IX2154.2
262700     PERFORM DE-LETE.                                             IX2154.2
262800 START-WRITE-GF-26.                                               IX2154.2
262900     PERFORM PRINT-DETAIL.                                        IX2154.2
263000 START-INIT-GF-27.                                                IX2154.2
263100     MOVE "----------" TO DUMMY-RECORD. PERFORM BLANK-LINE-PRINT. IX2154.2
263200     MOVE "START-TEST-GF-27" TO PAR-NAME.                         IX2154.2
263300     MOVE    "CCCCCCCCDD024"  TO IX-FD3-RECKEY-AREA.              IX2154.2
263400 START-TEST-GF-27.                                                IX2154.2
263500     READ    IX-FD3                                               IX2154.2
263600             INVALID KEY                                          IX2154.2
263700     MOVE "IX-28,F2 INVALID KEY PATH TAKEN ON INITIAL READ"       IX2154.2
263800                   TO RE-MARK                                     IX2154.2
263900             MOVE  "INVALID KEY ON READ" TO COMPUTED-A            IX2154.2
264000             MOVE 12 TO CORRECT-18V0                              IX2154.2
264100             GO TO START-FAIL-GF-27.                              IX2154.2
264200     MOVE    IX-FD3-240       TO FILE-RECORD-INFO (3).            IX2154.2
264300     IF      XRECORD-NUMBER (3) NOT EQUAL TO 12                   IX2154.2
264400             MOVE 12 TO RECNO                                     IX2154.2
264500             PERFORM DISPLAY-RECORD-KEYS                          IX2154.2
264600     MOVE "IX-28,F2 WRONG RECORD FOUND ON INITIAL READ" TO RE-MARKIX2154.2
264700             MOVE  XRECORD-NUMBER (3) TO COMPUTED-18V0            IX2154.2
264800             MOVE 4 TO CORRECT-18V0                               IX2154.2
264900             GO TO START-FAIL-GF-27.                              IX2154.2
265000     PERFORM START-INIT-FD3 THRU START-INIT-FD3-EXIT.             IX2154.2
265100     IF      INIT-FLAG NOT EQUAL ZERO                             IX2154.2
265200             GO TO START-DELETE-GF-27.                            IX2154.2
265300     MOVE    "CDCDCDCDCD100ALTKEY1" TO ALTERNATE-KEY1 (3).        IX2154.2
265400     MOVE    "DCDCDCDCDC100ALTKEY2" TO ALTERNATE-KEY2 (3).        IX2154.2
265500     MOVE    FILE-RECORD-INFO (3) TO IX-FD3R1-F-G-242.            IX2154.2
265600     REWRITE IX-FD3R1-F-G-242 INVALID KEY                         IX2154.2
265700     MOVE "IX-33;   INVALID KEY PATH TAKEN ON REWRITE" TO RE-MARK IX2154.2
265800             MOVE  "INVALID KEY; REWRITE" TO COMPUTED-A           IX2154.2
265900             MOVE 12 TO CORRECT-18V0                              IX2154.2
266000             GO TO START-FAIL-GF-27.                              IX2154.2
266100     PERFORM START-INIT-FD3 THRU START-INIT-FD3-EXIT.             IX2154.2
266200     IF      INIT-FLAG NOT EQUAL ZERO                             IX2154.2
266300             GO TO START-FAIL-GF-27.                              IX2154.2
266400     MOVE    "DCDCDCDCDC100ALTKEY2"  TO IX-FD3-ALTKEY2-AREA.      IX2154.2
266500     START   IX-FD3                                               IX2154.2
266600                    KEY IS EQUAL TO IX-FD3-KEY                    IX2154.2
266700             IN                                                   IX2154.2
266800             IX-FD3-ALTKEY2-AREA                                  IX2154.2
266900             INVALID KEY                                          IX2154.2
267000      MOVE "IX-36; INVALID KEY ON START OF REWRITTEN RECORD"      IX2154.2
267100             TO RE-MARK                                           IX2154.2
267200             MOVE  "INVALID KEY ON START" TO COMPUTED-A           IX2154.2
267300             MOVE 190 TO CORRECT-18V0                             IX2154.2
267400             GO TO START-FAIL-GF-27.                              IX2154.2
267500     READ    IX-FD3 NEXT RECORD AT END                            IX2154.2
267600     MOVE "IX-28;F1 AT END ON READ AFTER START" TO RE-MARK        IX2154.2
267700             MOVE "AT END ON READ" TO COMPUTED-A                  IX2154.2
267800             MOVE 190 TO CORRECT-18V0                             IX2154.2
267900             GO TO START-FAIL-GF-27.                              IX2154.2
268000     MOVE    IX-FD3-240        TO FILE-RECORD-INFO (1).           IX2154.2
268100     IF      XRECORD-NUMBER (1) NOT EQUAL TO 190                  IX2154.2
268200             MOVE ALTERNATE-KEY2 (1) TO WRK-FDW-ALTKEY2           IX2154.2
268300             MOVE FDW-ALTKEY2-1-20 TO COMPUTED-A                  IX2154.2
268400             MOVE "DCDCDCDCDC100" TO CORRECT-A                    IX2154.2
268500     MOVE                                                         IX2154.2
268600     "IX-28 OR IX-36; INCORR KEY FOUND ON FIRST READ DUPL KEYS"   IX2154.2
268700             TO RE-MARK                                           IX2154.2
268800             PERFORM PRINT-DETAIL                                 IX2154.2
268900     MOVE "WRONG RECORD NUMBER FOUND ON FIRST READ DUPLICATE KEYS"IX2154.2
269000             TO RE-MARK                                           IX2154.2
269100             MOVE XRECORD-NUMBER (1) TO COMPUTED-18V0             IX2154.2
269200             MOVE 190 TO CORRECT-18V0                             IX2154.2
269300             GO TO START-FAIL-GF-27.                              IX2154.2
269400     READ    IX-FD3 NEXT RECORD AT END                            IX2154.2
269500     MOVE "IX-28;F1 AT END ON READ AFTER FIRST READ" TO RE-MARK   IX2154.2
269600             MOVE "AT END ON READ" TO COMPUTED-A                  IX2154.2
269700             MOVE 12 TO CORRECT-18V0                              IX2154.2
269800             GO TO START-FAIL-GF-27.                              IX2154.2
269900     MOVE    IX-FD3-240       TO FILE-RECORD-INFO (1).            IX2154.2
270000     IF      XRECORD-NUMBER (1) EQUAL TO 12                       IX2154.2
270100             PERFORM PASS                                         IX2154.2
270200             GO TO START-WRITE-GF-27.                             IX2154.2
270300     PERFORM FAIL.                                                IX2154.2
270400     MOVE    ALTERNATE-KEY2 (1)  TO WRK-FDW-ALTKEY2.              IX2154.2
270500     MOVE    FDW-ALTKEY1-1-20  TO COMPUTED-A.                     IX2154.2
270600     MOVE                                                         IX2154.2
270700     "IX-28 OR IX-36; INCORR KEY FOUND ON SECOND READ DUPL KEYS"  IX2154.2
270800             TO RE-MARK.                                          IX2154.2
270900     PERFORM PRINT-DETAIL.                                        IX2154.2
271000     MOVE "WRONG REC NUMBER FOUND ON SECOND READ DUPLICATE KEYS"  IX2154.2
271100             TO RE-MARK.                                          IX2154.2
271200     MOVE    XRECORD-NUMBER (1) TO COMPUTED-18V0.                 IX2154.2
271300     MOVE    12 TO CORRECT-18V0.                                  IX2154.2
271400 START-FAIL-GF-27.                                                IX2154.2
271500     PERFORM FAIL.                                                IX2154.2
271600     GO TO   START-WRITE-GF-27.                                   IX2154.2
271700 START-DELETE-GF-27.                                              IX2154.2
271800     PERFORM DE-LETE.                                             IX2154.2
271900 START-WRITE-GF-27.                                               IX2154.2
272000     PERFORM PRINT-DETAIL.                                        IX2154.2
272100 START-TERM-GF.                                                   IX2154.2
272200     CLOSE   IX-FD3.                                              IX2154.2
272300 START-TEST-FINISH.                                               IX2154.2
272400     GO TO   START-TEST-COMPLETE.                                 IX2154.2
272500 START-INIT-FD1.                                                  IX2154.2
272600     MOVE    SPACE  TO FILE-RECORD-INFO (1).                      IX2154.2
272700     MOVE    ZERO  TO INIT-FLAG.                                  IX2154.2
272800     MOVE    9999  TO XRECORD-NUMBER (2).                         IX2154.2
272900     MOVE    SPACE TO IX-FD1R1-F-G-240.                           IX2154.2
273000     MOVE    "GGGGGGGGGG200" TO  FDW-RECKEY-1-13.                 IX2154.2
273100     MOVE    WRK-FDW-RECKEY TO IX-REC-KEY-AREA.                   IX2154.2
273200     START   IX-FD1  KEY IS EQUAL TO IX-FD1-KEY INVALID KEY       IX2154.2
273300                     MOVE  "INVALID KEY ON START" TO COMPUTED-A   IX2154.2
273400                     GO TO START-INIT-FD1-ERROR.                  IX2154.2
273500     READ    IX-FD1 NEXT RECORD INTO  FILE-RECORD-INFO (2)        IX2154.2
273600                     AT END  MOVE  "AT END ON READ" TO COMPUTED-A IX2154.2
273700                     GO TO START-INIT-FD1-ERROR.                  IX2154.2
273800     IF      XRECORD-NUMBER (2) EQUAL TO 100                      IX2154.2
273900             GO TO START-INIT-FD1-EXIT.                           IX2154.2
274000     MOVE    XRECORD-KEY (2)  TO WRK-FDW-RECKEY.                  IX2154.2
274100     MOVE    FDW-RECKEY-1-13  TO COMPUTED-A.                      IX2154.2
274200 START-INIT-FD1-ERROR.                                            IX2154.2
274300     MOVE     1 TO INIT-FLAG.                                     IX2154.2
274400     MOVE    "TEST IMPROPERLY INITIALIZED" TO RE-MARK.            IX2154.2
274500     MOVE    "GGGGGGGGGG200" TO CORRECT-A.                        IX2154.2
274600     PERFORM PRINT-DETAIL.                                        IX2154.2
274700 START-INIT-FD1-EXIT.                                             IX2154.2
274800     EXIT.                                                        IX2154.2
274900 START-INIT-FD2.                                                  IX2154.2
275000     MOVE    SPACE  TO FILE-RECORD-INFO (1).                      IX2154.2
275100     MOVE    ZERO  TO INIT-FLAG.                                  IX2154.2
275200     MOVE    9999  TO XRECORD-NUMBER (2).                         IX2154.2
275300     MOVE    SPACE TO IX-FD2R1-F-G-241.                           IX2154.2
275400     MOVE    "GGGGGGGGGG200" TO  IX-FD2-KEY.                      IX2154.2
275500     START   IX-FD2  KEY IS EQUAL TO IX-FD2-KEY INVALID KEY       IX2154.2
275600                     MOVE  "INVALID KEY ON START" TO COMPUTED-A   IX2154.2
275700                     GO TO START-INIT-FD2-ERROR.                  IX2154.2
275800     READ    IX-FD2 NEXT RECORD INTO  FILE-RECORD-INFO (2)        IX2154.2
275900                     AT END  MOVE  "AT END ON READ" TO COMPUTED-A IX2154.2
276000                     GO TO START-INIT-FD2-ERROR.                  IX2154.2
276100     IF      XRECORD-NUMBER (2) EQUAL TO 100                      IX2154.2
276200             GO TO START-INIT-FD2-EXIT.                           IX2154.2
276300     MOVE    XRECORD-KEY (2)  TO WRK-FDW-RECKEY.                  IX2154.2
276400     MOVE    FDW-RECKEY-1-13  TO COMPUTED-A.                      IX2154.2
276500 START-INIT-FD2-ERROR.                                            IX2154.2
276600     MOVE     1 TO INIT-FLAG.                                     IX2154.2
276700     MOVE    "TEST IMPROPERLY INITIALIZED" TO RE-MARK.            IX2154.2
276800     MOVE    "GGGGGGGGGG200" TO CORRECT-A.                        IX2154.2
276900     PERFORM PRINT-DETAIL.                                        IX2154.2
277000 START-INIT-FD2-EXIT.                                             IX2154.2
277100     EXIT.                                                        IX2154.2
277200 START-INIT-FD3.                                                  IX2154.2
277300     MOVE    SPACE  TO FILE-RECORD-INFO (1).                      IX2154.2
277400     MOVE    ZERO  TO INIT-FLAG.                                  IX2154.2
277500     MOVE    9999  TO XRECORD-NUMBER (2).                         IX2154.2
277600     MOVE    SPACE TO IX-FD3R1-F-G-242.                           IX2154.2
277700     MOVE    "GGGGGGGGGG200" TO  IX-FD3-RECKEY-AREA.              IX2154.2
277800     START   IX-FD3                                               IX2154.2
277900           INVALID KEY  MOVE  "INVALID KEY ON START" TO COMPUTED-AIX2154.2
278000                     GO TO START-INIT-FD3-ERROR.                  IX2154.2
278100     READ    IX-FD3 NEXT RECORD INTO FILE-RECORD-INFO (2)         IX2154.2
278200                     AT END  MOVE  "AT END ON READ" TO COMPUTED-A IX2154.2
278300                     GO TO START-INIT-FD3-ERROR.                  IX2154.2
278400     IF      XRECORD-NUMBER (2) EQUAL TO 100                      IX2154.2
278500             GO TO START-INIT-FD3-EXIT.                           IX2154.2
278600     MOVE    XRECORD-KEY (2)  TO WRK-FDW-RECKEY.                  IX2154.2
278700     MOVE    FDW-RECKEY-1-13  TO COMPUTED-A.                      IX2154.2
278800 START-INIT-FD3-ERROR.                                            IX2154.2
278900     MOVE     1 TO INIT-FLAG.                                     IX2154.2
279000     MOVE    "TEST IMPROPERLY INITIALIZED" TO RE-MARK.            IX2154.2
279100     MOVE    "GGGGGGGGGG200" TO CORRECT-A.                        IX2154.2
279200     PERFORM PRINT-DETAIL.                                        IX2154.2
279300 START-INIT-FD3-EXIT.                                             IX2154.2
279400     EXIT.                                                        IX2154.2
279500 DISPLAY-RECORD-KEYS.                                             IX2154.2
279600     MOVE    XRECORD-KEY (3)  TO WRK-FDW-RECKEY.                  IX2154.2
279700     MOVE    FDW-RECKEY-1-13  TO COMPUTED-A.                      IX2154.2
279800     MOVE    RECKEY-VALUE (RECNO)  TO CORRECT-A.                  IX2154.2
279900     MOVE    "RECORD KEY VALUES" TO RE-MARK.                      IX2154.2
280000     PERFORM PRINT-DETAIL.                                        IX2154.2
280100 START-TEST-COMPLETE.                                             IX2154.2
280200     EXIT.                                                        IX2154.2
280300 CCVS-EXIT SECTION.                                               IX2154.2
280400 CCVS-999999.                                                     IX2154.2
280500     GO TO CLOSE-FILES.                                           IX2154.2
      *END-OF,IX215A                                                                  
