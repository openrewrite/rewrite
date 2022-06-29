      *HEADER,COBOL,SQ401M                                                            
000100 IDENTIFICATION DIVISION.                                         SQ4014.2
000200 PROGRAM-ID.                                                      SQ4014.2
000300     SQ401M.                                                      SQ4014.2
000400*THE FOLLOWING PROGRAM TESTS THE FLAGGING OF HIGH                 SQ4014.2
000500*SUBSET FEATURES THAT ARE USED IN SEQUENTIAL                      SQ4014.2
000600*INPUT-OUTPUT.                                                    SQ4014.2
000700 ENVIRONMENT DIVISION.                                            SQ4014.2
000800 CONFIGURATION SECTION.                                           SQ4014.2
000900 SOURCE-COMPUTER.                                                 SQ4014.2
001000     XXXXX082.                                                    SQ4014.2
001100 OBJECT-COMPUTER.                                                 SQ4014.2
001200     XXXXX083.                                                    SQ4014.2
001300 INPUT-OUTPUT SECTION.                                            SQ4014.2
001400 FILE-CONTROL.                                                    SQ4014.2
001500     SELECT OPTIONAL TFIL ASSIGN                                  SQ4014.2
001600*Message expected for above statement: NON-CONFORMING STANDARD    SQ4014.2
001700                                                                  SQ4014.2
001800     XXXXX002                                                     SQ4014.2
001900         RESERVE 2 AREAS                                          SQ4014.2
002000*Message expected for above statement: NON-CONFORMING STANDARD    SQ4014.2
002100                                                                  SQ4014.2
002200         ORGANIZATION IS SEQUENTIAL                               SQ4014.2
002300         PADDING CHARACTER IS "P"                                 SQ4014.2
002400*Message expected for above statement: NON-CONFORMING STANDARD    SQ4014.2
002500                                                                  SQ4014.2
002600         RECORD DELIMITER IS STANDARD-1                           SQ4014.2
002700*Message expected for above statement: NON-CONFORMING STANDARD    SQ4014.2
002800                                                                  SQ4014.2
002900         ACCESS MODE IS SEQUENTIAL.                               SQ4014.2
003000                                                                  SQ4014.2
003100     SELECT TFIL2 ASSIGN                                          SQ4014.2
003200     XXXXX008                                                     SQ4014.2
003300         ORGANIZATION IS SEQUENTIAL                               SQ4014.2
003400         ACCESS MODE IS SEQUENTIAL.                               SQ4014.2
003500                                                                  SQ4014.2
003600                                                                  SQ4014.2
003700     SELECT TFIL3 ASSIGN                                          SQ4014.2
003800     XXXXX003.                                                    SQ4014.2
003900                                                                  SQ4014.2
004000 I-O-CONTROL.                                                     SQ4014.2
004100     SAME RECORD AREA FOR TFIL2, TFIL                             SQ4014.2
004200*Message expected for above statement: NON-CONFORMING STANDARD    SQ4014.2
004300                                                                  SQ4014.2
004400     MULTIPLE FILE TAPE CONTAINS TFIL2.                           SQ4014.2
004500*Message expected for above statement: NON-CONFORMING STANDARD    SQ4014.2
004600                                                                  SQ4014.2
004700 DATA DIVISION.                                                   SQ4014.2
004800 FILE SECTION.                                                    SQ4014.2
004900 FD TFIL                                                          SQ4014.2
005000     BLOCK CONTAINS 1 TO 8 RECORDS                                SQ4014.2
005100*Message expected for above statement: NON-CONFORMING STANDARD    SQ4014.2
005200                                                                  SQ4014.2
005300     RECORD VARYING IN SIZE FROM 1 TO 8 CHARACTERS                SQ4014.2
005400*Message expected for above statement: NON-CONFORMING STANDARD    SQ4014.2
005500                                                                  SQ4014.2
005600     LINAGE IS 20 LINES                                           SQ4014.2
005700*Message expected for above statement: NON-CONFORMING STANDARD    SQ4014.2
005800                                                                  SQ4014.2
005900     LABEL RECORDS ARE STANDARD                                   SQ4014.2
006000     VALUE OF                                                     SQ4014.2
006100     XXXXX074                                                     SQ4014.2
006200     IS VKEY.                                                     SQ4014.2
006300*Message expected for above statement: NON-CONFORMING STANDARD    SQ4014.2
006400                                                                  SQ4014.2
006500 01 FREC.                                                         SQ4014.2
006600     03 RKEY PIC 9(8).                                            SQ4014.2
006700                                                                  SQ4014.2
006800 FD TFIL2.                                                        SQ4014.2
006900 01 FREC2.                                                        SQ4014.2
007000     03 RKEY2 PIC 9(8).                                           SQ4014.2
007100                                                                  SQ4014.2
007200                                                                  SQ4014.2
007300 FD TFIL3.                                                        SQ4014.2
007400 01 FREC3.                                                        SQ4014.2
007500    02  RKEY3     PIC 9(8).                                       SQ4014.2
007600                                                                  SQ4014.2
007700 WORKING-STORAGE SECTION.                                         SQ4014.2
007800                                                                  SQ4014.2
007900 01 VARIABLES.                                                    SQ4014.2
008000                                                                  SQ4014.2
008100     03 VKEY                                                      SQ4014.2
008200     XXXXX086.                                                    SQ4014.2
008300                                                                  SQ4014.2
008400                                                                  SQ4014.2
008500 PROCEDURE DIVISION.                                              SQ4014.2
008600                                                                  SQ4014.2
008700 SQ401M-CONTROL.                                                  SQ4014.2
008800     OPEN INPUT TFIL.                                             SQ4014.2
008900     PERFORM SQ401M-CLOSEREMOV THRU SQ401M-WRITEEOP.              SQ4014.2
009000     CLOSE TFIL.                                                  SQ4014.2
009100     CLOSE TFIL2.                                                 SQ4014.2
009200     STOP RUN.                                                    SQ4014.2
009300                                                                  SQ4014.2
009400 SQ401M-CLOSEREMOV.                                               SQ4014.2
009500     CLOSE TFIL REEL FOR REMOVAL.                                 SQ4014.2
009600*Message expected for above statement: NON-CONFORMING STANDARD    SQ4014.2
009700                                                                  SQ4014.2
009800 SQ401M-CLOSENRW.                                                 SQ4014.2
009900     OPEN INPUT TFIL.                                             SQ4014.2
010000     CLOSE TFIL WITH NO REWIND.                                   SQ4014.2
010100*Message expected for above statement: NON-CONFORMING STANDARD    SQ4014.2
010200                                                                  SQ4014.2
010300 SQ401M-CLOSELOCK.                                                SQ4014.2
010400     OPEN INPUT TFIL.                                             SQ4014.2
010500     CLOSE TFIL WITH LOCK.                                        SQ4014.2
010600*Message expected for above statement: NON-CONFORMING STANDARD    SQ4014.2
010700                                                                  SQ4014.2
010800 SQ401M-OPENREV.                                                  SQ4014.2
010900     OPEN INPUT TFIL REVERSED.                                    SQ4014.2
011000*Message expected for above statement: NON-CONFORMING STANDARD    SQ4014.2
011100                                                                  SQ4014.2
011200 SQ401M-OPENNOREW.                                                SQ4014.2
011300     CLOSE TFIL.                                                  SQ4014.2
011400     OPEN INPUT TFIL WITH NO REWIND.                              SQ4014.2
011500*Message expected for above statement: NON-CONFORMING STANDARD    SQ4014.2
011600                                                                  SQ4014.2
011700 SQ401M-EXTEND.                                                   SQ4014.2
011800     CLOSE TFIL.                                                  SQ4014.2
011900     OPEN EXTEND TFIL3.                                           SQ4014.2
012000*Message expected for above statement: NON-CONFORMING STANDARD    SQ4014.2
012100                                                                  SQ4014.2
012200 SQ401M-READNEXT.                                                 SQ4014.2
012300     OPEN INPUT TFIL.                                             SQ4014.2
012400     READ TFIL NEXT RECORD                                        SQ4014.2
012500         AT END DISPLAY "AT END".                                 SQ4014.2
012600*Message expected for above statement: NON-CONFORMING STANDARD    SQ4014.2
012700                                                                  SQ4014.2
012800     CLOSE TFIL.                                                  SQ4014.2
012900                                                                  SQ4014.2
013000 SQ401M-WRITEEOP.                                                 SQ4014.2
013100     OPEN OUTPUT TFIL.                                            SQ4014.2
013200     WRITE FREC AT END-OF-PAGE DISPLAY "HELLO".                   SQ4014.2
013300*Message expected for above statement: NON-CONFORMING STANDARD    SQ4014.2
013400                                                                  SQ4014.2
013500                                                                  SQ4014.2
013600                                                                  SQ4014.2
013700*TOTAL NUMBER OF FLAGS EXPECTED = 18.                             SQ4014.2
      *END-OF,SQ401M                                                                  
