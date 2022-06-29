      *HEADER,COBOL,IC114A,SUBRTN,IC115A                                              
000100 IDENTIFICATION DIVISION.                                         IC1154.2
000200 PROGRAM-ID.                                                      IC1154.2
000300     IC115A.                                                      IC1154.2
000400****************************************************************  IC1154.2
000500*                                                              *  IC1154.2
000600*    VALIDATION FOR:-                                          *  IC1154.2
000700*                                                              *  IC1154.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IC1154.2
000900*                                                              *  IC1154.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".IC1154.2
001100*                                                              *  IC1154.2
001200****************************************************************  IC1154.2
001300*                                                              *  IC1154.2
001400*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  IC1154.2
001500*                                                              *  IC1154.2
001600*        X-55  - SYSTEM PRINTER NAME.                          *  IC1154.2
001700*        X-82  - SOURCE COMPUTER NAME.                         *  IC1154.2
001800*        X-83  - OBJECT COMPUTER NAME.                         *  IC1154.2
001900*                                                              *  IC1154.2
002000****************************************************************  IC1154.2
002100*                                                                 IC1154.2
002200*         THE ROUTINE IC115 IS A SUBPROGRAM CALLED BY IC114.      IC1154.2
002300*    THIS SUBPROGRAM CONTAINS A FILE SECTION, A WORKING-STORAGE   IC1154.2
002400*    SECTION AND A LINKAGE SECTION.  THE FILE SQ-FS3 IS CREATED   IC1154.2
002500*    AND VERIFIED IN THIS ROUTINE.  THE FILE IS OPENED AND READ   IC1154.2
002600*    AGAIN.  EACH RECORD IS CHECKED BY MOVING IT TO THE LINKAGE   IC1154.2
002700*    SECTION AND RETURNING TO THE MAIN PROGRAM TO VERIFY THE      IC1154.2
002800*    RECORD CONTENTS.  THE PRINTING OF THE OUTPUT REPORT FOR THE  IC1154.2
002900*    TEST RESULTS IS PERFORMED BY RETURNING TO THE MAIN PROGRAM   IC1154.2
003000*    IC114.                                                       IC1154.2
003100*                                                                 IC1154.2
003200*         THIS SUBPROGRAM IS ADAPTED FROM THE SEQUENTIAL I-O      IC1154.2
003300*    ROUTINE SQ104.  IF THAT ROUTINE DOES NOT PERFORM CORRECTLY   IC1154.2
003400*    THEN THE RESULTS OF THESE TESTS ARE INCONCLUSIVE.            IC1154.2
003500*                                                                 IC1154.2
003600********************************************                      IC1154.2
003700 ENVIRONMENT DIVISION.                                            IC1154.2
003800 CONFIGURATION SECTION.                                           IC1154.2
003900 SOURCE-COMPUTER.                                                 IC1154.2
004000     XXXXX082.                                                    IC1154.2
004100 OBJECT-COMPUTER.                                                 IC1154.2
004200     XXXXX083.                                                    IC1154.2
004300 INPUT-OUTPUT SECTION.                                            IC1154.2
004400 FILE-CONTROL.                                                    IC1154.2
004500     SELECT SQ-FS3 ASSIGN TO                                      IC1154.2
004600     XXXXX014                                                     IC1154.2
004700     ORGANIZATION IS SEQUENTIAL                                   IC1154.2
004800     ACCESS MODE IS SEQUENTIAL.                                   IC1154.2
004900 DATA DIVISION.                                                   IC1154.2
005000 FILE SECTION.                                                    IC1154.2
005100 FD  SQ-FS3                                                       IC1154.2
005200     BLOCK CONTAINS 120 CHARACTERS                                IC1154.2
005300     RECORD CONTAINS 120 CHARACTERS                               IC1154.2
005400     LABEL RECORDS ARE STANDARD                                   IC1154.2
005500C    VALUE OF                                                     IC1154.2
005600C    XXXXX074                                                     IC1154.2
005700C    IS                                                           IC1154.2
005800C    XXXXX075                                                     IC1154.2
005900*    XXXXX069                                                     IC1154.2
006000     DATA RECORD SQ-FS3R1-F-G-120.                                IC1154.2
006100 01  SQ-FS3R1-F-G-120.                                            IC1154.2
006200     02  FILLER PIC X(120).                                       IC1154.2
006300 WORKING-STORAGE SECTION.                                         IC1154.2
006400 01  FILE-RECORD-INFORMATION-REC.                                 IC1154.2
006500     03 FILE-RECORD-INFO-SKELETON.                                IC1154.2
006600        05 FILLER                 PICTURE X(48)       VALUE       IC1154.2
006700             "FILE=      ,RECORD=      /0,RECNO=000000,UPDT=00".  IC1154.2
006800        05 FILLER                 PICTURE X(46)       VALUE       IC1154.2
006900             ",ODO=0000,PGM=     ,LRECL=000000,BLKSIZ  =0000".    IC1154.2
007000        05 FILLER                 PICTURE X(26)       VALUE       IC1154.2
007100             ",LFIL=000000,ORG=  ,LBLR= ".                        IC1154.2
007200        05 FILLER                 PICTURE X(37)       VALUE       IC1154.2
007300             ",RECKEY=                             ".             IC1154.2
007400        05 FILLER                 PICTURE X(38)       VALUE       IC1154.2
007500             ",ALTKEY1=                             ".            IC1154.2
007600        05 FILLER                 PICTURE X(38)       VALUE       IC1154.2
007700             ",ALTKEY2=                             ".            IC1154.2
007800        05 FILLER                 PICTURE X(7)        VALUE SPACE.IC1154.2
007900     03 FILE-RECORD-INFO          OCCURS  10  TIMES.              IC1154.2
008000        05 FILE-RECORD-INFO-P1-120.                               IC1154.2
008100           07 FILLER              PIC X(5).                       IC1154.2
008200           07 XFILE-NAME           PIC X(6).                      IC1154.2
008300           07 FILLER              PIC X(8).                       IC1154.2
008400           07 XRECORD-NAME         PIC X(6).                      IC1154.2
008500           07 FILLER              PIC X(1).                       IC1154.2
008600           07 REELUNIT-NUMBER     PIC 9(1).                       IC1154.2
008700           07 FILLER              PIC X(7).                       IC1154.2
008800           07 XRECORD-NUMBER       PIC 9(6).                      IC1154.2
008900           07 FILLER              PIC X(6).                       IC1154.2
009000           07 UPDATE-NUMBER       PIC 9(2).                       IC1154.2
009100           07 FILLER              PIC X(5).                       IC1154.2
009200           07 ODO-NUMBER          PIC 9(4).                       IC1154.2
009300           07 FILLER              PIC X(5).                       IC1154.2
009400           07 XPROGRAM-NAME        PIC X(5).                      IC1154.2
009500           07 FILLER              PIC X(7).                       IC1154.2
009600           07 XRECORD-LENGTH       PIC 9(6).                      IC1154.2
009700           07 FILLER              PIC X(7).                       IC1154.2
009800           07 CHARS-OR-RECORDS    PIC X(2).                       IC1154.2
009900           07 FILLER              PIC X(1).                       IC1154.2
010000           07 XBLOCK-SIZE          PIC 9(4).                      IC1154.2
010100           07 FILLER              PIC X(6).                       IC1154.2
010200           07 RECORDS-IN-FILE     PIC 9(6).                       IC1154.2
010300           07 FILLER              PIC X(5).                       IC1154.2
010400           07 XFILE-ORGANIZATION   PIC X(2).                      IC1154.2
010500           07 FILLER              PIC X(6).                       IC1154.2
010600           07 XLABEL-TYPE          PIC X(1).                      IC1154.2
010700        05 FILE-RECORD-INFO-P121-240.                             IC1154.2
010800           07 FILLER              PIC X(8).                       IC1154.2
010900           07 XRECORD-KEY          PIC X(29).                     IC1154.2
011000           07 FILLER              PIC X(9).                       IC1154.2
011100           07 ALTERNATE-KEY1      PIC X(29).                      IC1154.2
011200           07 FILLER              PIC X(9).                       IC1154.2
011300           07 ALTERNATE-KEY2      PIC X(29).                      IC1154.2
011400           07 FILLER              PIC X(7).                       IC1154.2
011500 01  CCVS-PGM-ID PIC X(6) VALUE                                   IC1154.2
011600     "IC115A".                                                    IC1154.2
011700 LINKAGE SECTION.                                                 IC1154.2
011800 01  GROUP-LINKAGE-VARIABLES.                                     IC1154.2
011900         02  COUNT-OF-RECORDS  PICTURE 9(6).                      IC1154.2
012000     02  RECORDS-IN-ERROR  PICTURE 9(6).                          IC1154.2
012100     02  ERROR-FLAG  PICTURE 9.                                   IC1154.2
012200     02  EOF-FLAG  PICTURE 9.                                     IC1154.2
012300     02  CALL-FLAG  PICTURE 9.                                    IC1154.2
012400 01  FILE-REC-SQ-FS3.                                             IC1154.2
012500     02  FILLER  PICTURE X(120).                                  IC1154.2
012600 PROCEDURE DIVISION USING GROUP-LINKAGE-VARIABLES                 IC1154.2
012700                          FILE-REC-SQ-FS3.                        IC1154.2
012800 SECT-IC115-0001 SECTION.                                         IC1154.2
012900 INIT-PARAGRAPH.                                                  IC1154.2
013000     GO TO SECT-IC115-0002 SECT-IC115-0003 SECT-IC115-0004        IC1154.2
013100           SECT-IC115-0005 SECT-IC115-0006                        IC1154.2
013200              DEPENDING ON CALL-FLAG.                             IC1154.2
013300*        THE IDENTIFIER CALL-FLAG CONTROLS THE SUBPROGRAM         IC1154.2
013400*    PROCESSING AND IT IS SET BY THE CALLING PROGRAM IC114.       IC1154.2
013500 SECT-IC115-0002 SECTION.                                         IC1154.2
013600 LINK-INIT-10.                                                    IC1154.2
013700     MOVE FILE-RECORD-INFO-SKELETON TO                            IC1154.2
013800              FILE-RECORD-INFO (1).                               IC1154.2
013900     MOVE "SQ-FS3" TO XFILE-NAME (1).                             IC1154.2
014000     MOVE "R1-F-G" TO XRECORD-NAME (1).                           IC1154.2
014100     MOVE CCVS-PGM-ID TO XPROGRAM-NAME (1).                       IC1154.2
014200     MOVE 120 TO XRECORD-LENGTH (1).                              IC1154.2
014300     MOVE "CH" TO CHARS-OR-RECORDS (1).                           IC1154.2
014400     MOVE 120 TO XBLOCK-SIZE (1).                                 IC1154.2
014500     MOVE 000649 TO RECORDS-IN-FILE (1).                          IC1154.2
014600     MOVE "SQ" TO XFILE-ORGANIZATION (1).                         IC1154.2
014700     MOVE "S" TO XLABEL-TYPE (1).                                 IC1154.2
014800     MOVE 000001 TO XRECORD-NUMBER (1).                           IC1154.2
014900     OPEN OUTPUT SQ-FS3.                                          IC1154.2
015000 LINK-TEST-10.                                                    IC1154.2
015100     MOVE FILE-RECORD-INFO-P1-120 (1) TO SQ-FS3R1-F-G-120.        IC1154.2
015200     WRITE SQ-FS3R1-F-G-120.                                      IC1154.2
015300     IF XRECORD-NUMBER (1) EQUAL TO 649                           IC1154.2
015400              GO TO LINK-TEST-10-01.                              IC1154.2
015500     ADD 1 TO XRECORD-NUMBER (1).                                 IC1154.2
015600     GO TO LINK-TEST-10.                                          IC1154.2
015700 LINK-TEST-10-01.                                                 IC1154.2
015800     CLOSE SQ-FS3.                                                IC1154.2
015900     MOVE XRECORD-NUMBER (1) TO COUNT-OF-RECORDS.                 IC1154.2
016000*         A MASS STORAGE SEQUENTIAL FILE WITH 120 CHARACTERS PER  IC1154.2
016100*    RECORD HAS BEEN CREATED.  THE FILE CONTAINS 649 RECORDS.     IC1154.2
016200 LINK-EXIT-10.                                                    IC1154.2
016300     EXIT PROGRAM.                                                IC1154.2
016400 SECT-IC115-0003 SECTION.                                         IC1154.2
016500 LINK-INIT-11.                                                    IC1154.2
016600*         THIS TEST READS AND CHECKS THE FILE CREATED IN          IC1154.2
016700*    SECT-IC115-0002.                                             IC1154.2
016800     OPEN INPUT SQ-FS3.                                           IC1154.2
016900 LINK-TEST-11.                                                    IC1154.2
017000     READ SQ-FS3 RECORD                                           IC1154.2
017100              AT END GO TO LINK-CLOSE-11.                         IC1154.2
017200     MOVE SQ-FS3R1-F-G-120 TO FILE-RECORD-INFO-P1-120 (1).        IC1154.2
017300     ADD 1 TO COUNT-OF-RECORDS.                                   IC1154.2
017400     IF COUNT-OF-RECORDS GREATER THAN 649                         IC1154.2
017500              MOVE 1 TO ERROR-FLAG                                IC1154.2
017600              GO TO LINK-CLOSE-11.                                IC1154.2
017700     IF COUNT-OF-RECORDS NOT EQUAL TO XRECORD-NUMBER (1)          IC1154.2
017800              GO TO LINK-FAIL-11.                                 IC1154.2
017900     IF XFILE-NAME (1) NOT EQUAL TO "SQ-FS3"                      IC1154.2
018000              GO TO LINK-FAIL-11.                                 IC1154.2
018100     IF XLABEL-TYPE (1) NOT EQUAL TO "S"                          IC1154.2
018200              GO TO LINK-FAIL-11.                                 IC1154.2
018300     GO TO LINK-TEST-11.                                          IC1154.2
018400 LINK-FAIL-11.                                                    IC1154.2
018500     ADD 1 TO RECORDS-IN-ERROR.                                   IC1154.2
018600     MOVE 1 TO ERROR-FLAG.                                        IC1154.2
018700 LINK-CLOSE-11.                                                   IC1154.2
018800     CLOSE SQ-FS3.                                                IC1154.2
018900 LINK-EXIT-11.                                                    IC1154.2
019000     EXIT PROGRAM.                                                IC1154.2
019100 SECT-IC115-0004 SECTION.                                         IC1154.2
019200 LINK-INIT-12.                                                    IC1154.2
019300     OPEN INPUT SQ-FS3.                                           IC1154.2
019400 LINK-INIT-12-EXIT.                                               IC1154.2
019500     EXIT PROGRAM.                                                IC1154.2
019600 SECT-IC115-0005 SECTION.                                         IC1154.2
019700 LINK-TEST-12.                                                    IC1154.2
019800     READ SQ-FS3 RECORD                                           IC1154.2
019900           AT END MOVE 1 TO EOF-FLAG.                             IC1154.2
020000     MOVE SQ-FS3R1-F-G-120 TO FILE-REC-SQ-FS3.                    IC1154.2
020100*                                                                 IC1154.2
020200*         THE MAIN PROGRAM IC114 REPEATLY CALLS THE SUBPROGRAM    IC1154.2
020300*    IC115 TO READ THE FILE SQ-FS3.  THE DATA RECORD IS MOVED     IC1154.2
020400*    TO A LINKAGE RECORD FOR CHECKING OF THE CONTENTS BY THE      IC1154.2
020500*    MAIN PROGRAM.                                                IC1154.2
020600*                                                                 IC1154.2
020700 LINK-EXIT-12.                                                    IC1154.2
020800     EXIT PROGRAM.                                                IC1154.2
020900 SECT-IC115-0006 SECTION.                                         IC1154.2
021000 LINK-CLOSE-12.                                                   IC1154.2
021100     CLOSE SQ-FS3.                                                IC1154.2
021200 LINK-CLOSE-EXIT-12.                                              IC1154.2
021300     EXIT PROGRAM.                                                IC1154.2
      *END-OF,IC115A                                                                  
