      *HEADER,COBOL,IX301M                                                            
000100 IDENTIFICATION DIVISION.                                         IX3014.2
000200 PROGRAM-ID.                                                      IX3014.2
000300      IX301M.                                                     IX3014.2
000400*THE FOLLOWING PROGRAM TESTS THE FLAGGING OF INTERMEDIATE         IX3014.2
000500*SUBSET FEATURES THAT ARE USED IN LEVEL 1 INDEXED                 IX3014.2
000600*INPUT-OUTPUT.                                                    IX3014.2
000700 ENVIRONMENT DIVISION.                                            IX3014.2
000800 CONFIGURATION SECTION.                                           IX3014.2
000900 SOURCE-COMPUTER.                                                 IX3014.2
001000     XXXXX082.                                                    IX3014.2
001100 OBJECT-COMPUTER.                                                 IX3014.2
001200     XXXXX083.                                                    IX3014.2
001300 INPUT-OUTPUT SECTION.                                            IX3014.2
001400 FILE-CONTROL.                                                    IX3014.2
001500     SELECT TFIL ASSIGN                                           IX3014.2
001600     XXXXX024                                                     IX3014.2
001700            ORGANIZATION IS INDEXED                               IX3014.2
001800*Message expected for above statement: NON-CONFORMING STANDARD    IX3014.2
001900                                                                  IX3014.2
002000            ACCESS MODE IS RANDOM                                 IX3014.2
002100*Message expected for above statement: NON-CONFORMING STANDARD    IX3014.2
002200                                                                  IX3014.2
002300            RECORD KEY IS RKEY.                                   IX3014.2
002400*Message expected for above statement: NON-CONFORMING STANDARD    IX3014.2
002500                                                                  IX3014.2
002600 DATA DIVISION.                                                   IX3014.2
002700 FILE SECTION.                                                    IX3014.2
002800 FD  TFIL.                                                        IX3014.2
002900 01  FREC.                                                        IX3014.2
003000     03 RKEY  PIC X(8).                                           IX3014.2
003100                                                                  IX3014.2
003200 WORKING-STORAGE SECTION.                                         IX3014.2
003300 01  VARIABLES.                                                   IX3014.2
003400   03  STATE     PIC X(4) VALUE SPACES.                           IX3014.2
003500                                                                  IX3014.2
003600 PROCEDURE DIVISION.                                              IX3014.2
003700                                                                  IX3014.2
003800 IX301M-CONTROL.                                                  IX3014.2
003900     OPEN I-O TFIL.                                               IX3014.2
004000     PERFORM IX301M-READ THRU IX301M-DELETE 1 TIMES.              IX3014.2
004100     CLOSE TFIL.                                                  IX3014.2
004200     STOP RUN.                                                    IX3014.2
004300                                                                  IX3014.2
004400 IX301M-READ.                                                     IX3014.2
004500     READ TFIL INVALID KEY PERFORM INV-PARA                       IX3014.2
004600                 NOT INVALID KEY PERFORM DONE-PARA.               IX3014.2
004700*Message expected for above statement: NON-CONFORMING STANDARD    IX3014.2
004800                                                                  IX3014.2
004900 IX301M-REWRITE.                                                  IX3014.2
005000     REWRITE FREC INVALID KEY PERFORM INV-PARA                    IX3014.2
005100                 NOT INVALID KEY PERFORM DONE-PARA.               IX3014.2
005200*Message expected for above statement: NON-CONFORMING STANDARD    IX3014.2
005300                                                                  IX3014.2
005400 IX301M-WRITE.                                                    IX3014.2
005500     WRITE FREC INVALID KEY PERFORM INV-PARA                      IX3014.2
005600                 NOT INVALID KEY PERFORM DONE-PARA.               IX3014.2
005700*Message expected for above statement: NON-CONFORMING STANDARD    IX3014.2
005800                                                                  IX3014.2
005900 IX301M-DELETE.                                                   IX3014.2
006000     DELETE TFIL INVALID KEY PERFORM INV-PARA                     IX3014.2
006100                 NOT INVALID KEY PERFORM DONE-PARA.               IX3014.2
006200*Message expected for above statement: NON-CONFORMING STANDARD    IX3014.2
006300                                                                  IX3014.2
006400 INV-PARA.                                                        IX3014.2
006500     MOVE "INVA" TO STATE.                                        IX3014.2
006600                                                                  IX3014.2
006700 DONE-PARA.                                                       IX3014.2
006800     MOVE "DONE" TO STATE.                                        IX3014.2
006900                                                                  IX3014.2
007000*TOTAL NUMBER OF FLAGS EXPECTED = 7.                              IX3014.2
      *END-OF,IX301M                                                                  
