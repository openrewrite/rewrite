      *HEADER,COBOL,RL401M                                                            
000100 IDENTIFICATION DIVISION.                                         RL4014.2
000200 PROGRAM-ID.                                                      RL4014.2
000300      RL401M.                                                     RL4014.2
000400*THE FOLLOWING PROGRAM TESTS THE FLAGGING OF                      RL4014.2
000500*HIGH SUBSET FEATURES THAT ARE USED IN RELATIVE                   RL4014.2
000600*INPUT-OUTPUT.                                                    RL4014.2
000700 ENVIRONMENT DIVISION.                                            RL4014.2
000800 CONFIGURATION SECTION.                                           RL4014.2
000900 SOURCE-COMPUTER.                                                 RL4014.2
001000     XXXXX082.                                                    RL4014.2
001100 OBJECT-COMPUTER.                                                 RL4014.2
001200     XXXXX083.                                                    RL4014.2
001300 INPUT-OUTPUT SECTION.                                            RL4014.2
001400 FILE-CONTROL.                                                    RL4014.2
001500     SELECT OPTIONAL TFIL ASSIGN                                  RL4014.2
001600*Message expected for above statement: NON-CONFORMING STANDARD    RL4014.2
001700     XXXXX021                                                     RL4014.2
001800         RESERVE 2 AREAS                                          RL4014.2
001900*Message expected for above statement: NON-CONFORMING STANDARD    RL4014.2
002000         ORGANIZATION IS RELATIVE                                 RL4014.2
002100         ACCESS MODE IS DYNAMIC                                   RL4014.2
002200*Message expected for above statement: NON-CONFORMING STANDARD    RL4014.2
002300         RELATIVE KEY IS RKEY.                                    RL4014.2
002400                                                                  RL4014.2
002500     SELECT TFIL2 ASSIGN                                          RL4014.2
002600     XXXXX022                                                     RL4014.2
002700         ORGANIZATION IS RELATIVE.                                RL4014.2
002800                                                                  RL4014.2
002900 I-O-CONTROL.                                                     RL4014.2
003000     SAME RECORD AREA FOR TFIL2, TFIL.                            RL4014.2
003100*Message expected for above statement: NON-CONFORMING STANDARD    RL4014.2
003200                                                                  RL4014.2
003300 DATA DIVISION.                                                   RL4014.2
003400 FILE SECTION.                                                    RL4014.2
003500 FD TFIL                                                          RL4014.2
003600     RECORD IS VARYING IN SIZE FROM 1 TO 8 CHARACTERS.            RL4014.2
003700*Message expected for above statement: NON-CONFORMING STANDARD    RL4014.2
003800                                                                  RL4014.2
003900 01 FREC.                                                         RL4014.2
004000     03 GUBBINS PIC 9(8).                                         RL4014.2
004100                                                                  RL4014.2
004200                                                                  RL4014.2
004300 FD TFIL2.                                                        RL4014.2
004400 01 FREC2.                                                        RL4014.2
004500     03 RKEY2 PIC 9(8).                                           RL4014.2
004600                                                                  RL4014.2
004700 WORKING-STORAGE SECTION.                                         RL4014.2
004800 01 RKEY PIC 9(8) VALUE ZERO.                                     RL4014.2
004900                                                                  RL4014.2
005000 PROCEDURE DIVISION.                                              RL4014.2
005100                                                                  RL4014.2
005200 RL401M-CONTROL.                                                  RL4014.2
005300     OPEN INPUT TFIL.                                             RL4014.2
005400     PERFORM RL401M-CLOSE THRU RL401M-START.                      RL4014.2
005500     CLOSE TFIL.                                                  RL4014.2
005600     CLOSE TFIL2.                                                 RL4014.2
005700     STOP RUN.                                                    RL4014.2
005800                                                                  RL4014.2
005900 RL401M-CLOSE.                                                    RL4014.2
006000     CLOSE TFIL WITH LOCK.                                        RL4014.2
006100*Message expected for above statement: NON-CONFORMING STANDARD    RL4014.2
006200     OPEN INPUT TFIL.                                             RL4014.2
006300                                                                  RL4014.2
006400 RL401M-OPENEXT.                                                  RL4014.2
006500     OPEN EXTEND TFIL2.                                           RL4014.2
006600*Message expected for above statement: NON-CONFORMING STANDARD    RL4014.2
006700                                                                  RL4014.2
006800 RL401M-READNEXT.                                                 RL4014.2
006900     READ TFIL NEXT RECORD                                        RL4014.2
007000         AT END DISPLAY "AT END".                                 RL4014.2
007100*Message expected for above statement: NON-CONFORMING STANDARD    RL4014.2
007200                                                                  RL4014.2
007300 RL401M-START.                                                    RL4014.2
007400     START TFIL KEY IS EQUAL TO RKEY                              RL4014.2
007500         INVALID KEY STOP RUN.                                    RL4014.2
007600*Message expected for above statement: NON-CONFORMING STANDARD    RL4014.2
007700                                                                  RL4014.2
007800*TOTAL NUMBER OF FLAGS EXPECTED = 9.                              RL4014.2
      *END-OF,RL401M                                                                  
