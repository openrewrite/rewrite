      *HEADER,COBOL,IX401M                                                            
000100 IDENTIFICATION DIVISION.                                         IX4014.2
000200 PROGRAM-ID.                                                      IX4014.2
000300     IX401M.                                                      IX4014.2
000400*THE FOLLOWING PROGRAM TESTS THE FLAGGING OF HIGH                 IX4014.2
000500*SUBSET FEATURES THAT ARE USED IN INDEXED                         IX4014.2
000600*INPUT-OUTPUT.                                                    IX4014.2
000700 ENVIRONMENT DIVISION.                                            IX4014.2
000800 CONFIGURATION SECTION.                                           IX4014.2
000900 SOURCE-COMPUTER.                                                 IX4014.2
001000     XXXXX082.                                                    IX4014.2
001100 OBJECT-COMPUTER.                                                 IX4014.2
001200     XXXXX083.                                                    IX4014.2
001300 INPUT-OUTPUT SECTION.                                            IX4014.2
001400 FILE-CONTROL.                                                    IX4014.2
001500     SELECT OPTIONAL TFIL ASSIGN                                  IX4014.2
001600*Message expected for above statement: NON-CONFORMING STANDARD    IX4014.2
001700                                                                  IX4014.2
001800     XXXXX025                                                     IX4014.2
001900         RESERVE 2 AREAS                                          IX4014.2
002000*Message expected for above statement: NON-CONFORMING STANDARD    IX4014.2
002100                                                                  IX4014.2
002200         ORGANIZATION IS INDEXED                                  IX4014.2
002300         ACCESS MODE IS DYNAMIC                                   IX4014.2
002400*Message expected for above statement: NON-CONFORMING STANDARD    IX4014.2
002500                                                                  IX4014.2
002600         RECORD KEY IS RKEY                                       IX4014.2
002700         ALTERNATE RECORD KEY IS BEANO.                           IX4014.2
002800*Message expected for above statement: NON-CONFORMING STANDARD    IX4014.2
002900                                                                  IX4014.2
003000     SELECT TFIL2 ASSIGN                                          IX4014.2
003100     XXXXX026                                                     IX4014.2
003200         ORGANIZATION IS INDEXED                                  IX4014.2
003300         ACCESS MODE IS SEQUENTIAL                                IX4014.2
003400         RECORD KEY IS RKEY2.                                     IX4014.2
003500                                                                  IX4014.2
003600 DATA DIVISION.                                                   IX4014.2
003700 FILE SECTION.                                                    IX4014.2
003800 FD TFIL                                                          IX4014.2
003900     RECORD IS VARYING IN SIZE FROM 18 TO 36 CHARACTERS.          IX4014.2
004000*Message expected for above statement: NON-CONFORMING STANDARD    IX4014.2
004100                                                                  IX4014.2
004200 01 FREC.                                                         IX4014.2
004300     03 RKEY PIC X(8).                                            IX4014.2
004400     03 BEANO PIC X(10).                                          IX4014.2
004500                                                                  IX4014.2
004600 FD TFIL2.                                                        IX4014.2
004700 01 FREC2.                                                        IX4014.2
004800     03 RKEY2 PIC X(8).                                           IX4014.2
004900                                                                  IX4014.2
005000 PROCEDURE DIVISION.                                              IX4014.2
005100                                                                  IX4014.2
005200 IX401M-CONTROL.                                                  IX4014.2
005300     OPEN INPUT TFIL.                                             IX4014.2
005400     PERFORM IX401M-CLOSE THRU IX401M-START.                      IX4014.2
005500     CLOSE TFIL.                                                  IX4014.2
005600     CLOSE TFIL2.                                                 IX4014.2
005700     STOP RUN.                                                    IX4014.2
005800                                                                  IX4014.2
005900 IX401M-CLOSE.                                                    IX4014.2
006000     CLOSE TFIL WITH LOCK.                                        IX4014.2
006100*Message expected for above statement: NON-CONFORMING STANDARD    IX4014.2
006200                                                                  IX4014.2
006300 IX401M-OPENEXT.                                                  IX4014.2
006400     OPEN EXTEND TFIL2.                                           IX4014.2
006500*Message expected for above statement: NON-CONFORMING STANDARD    IX4014.2
006600                                                                  IX4014.2
006700 IX401M-READNEXT.                                                 IX4014.2
006800     OPEN INPUT TFIL.                                             IX4014.2
006900     READ TFIL NEXT RECORD                                        IX4014.2
007000         AT END DISPLAY "AT END".                                 IX4014.2
007100*Message expected for above statement: NON-CONFORMING STANDARD    IX4014.2
007200                                                                  IX4014.2
007300 IX401M-READKEY.                                                  IX4014.2
007400     READ TFIL RECORD                                             IX4014.2
007500         KEY IS RKEY                                              IX4014.2
007600     INVALID KEY DISPLAY "INVALID".                               IX4014.2
007700*Message expected for above statement: NON-CONFORMING STANDARD    IX4014.2
007800                                                                  IX4014.2
007900 IX401M-START.                                                    IX4014.2
008000     START TFIL KEY IS EQUAL TO RKEY                              IX4014.2
008100         INVALID KEY DISPLAY "INVALID".                           IX4014.2
008200*Message expected for above statement: NON-CONFORMING STANDARD    IX4014.2
008300                                                                  IX4014.2
008400*TOTAL NUMBER OF FLAGS EXPECTED = 10.                             IX4014.2
      *END-OF,IX401M                                                                  
