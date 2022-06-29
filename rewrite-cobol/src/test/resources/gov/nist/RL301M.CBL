      *HEADER,COBOL,RL301M                                                            
000100 IDENTIFICATION DIVISION.                                         RL3014.2
000200 PROGRAM-ID.                                                      RL3014.2
000300      RL301M.                                                     RL3014.2
000400*The following program tests the flagging of intermediate         RL3014.2
000500*subset features that are used in relative                        RL3014.2
000600*input-output.                                                    RL3014.2
000700 ENVIRONMENT DIVISION.                                            RL3014.2
000800 CONFIGURATION SECTION.                                           RL3014.2
000900 SOURCE-COMPUTER.                                                 RL3014.2
001000     XXXXX082.                                                    RL3014.2
001100 OBJECT-COMPUTER.                                                 RL3014.2
001200     XXXXX083.                                                    RL3014.2
001300 INPUT-OUTPUT SECTION.                                            RL3014.2
001400 FILE-CONTROL.                                                    RL3014.2
001500     SELECT TFIL ASSIGN                                           RL3014.2
001600     XXXXX021                                                     RL3014.2
001700            ORGANIZATION IS RELATIVE                              RL3014.2
001800*Message expected for above statement: NON-CONFORMING STANDARD    RL3014.2
001900            ACCESS MODE IS RANDOM                                 RL3014.2
002000*Message expected for above statement: NON-CONFORMING STANDARD    RL3014.2
002100            RELATIVE KEY IS RKEY.                                 RL3014.2
002200 DATA DIVISION.                                                   RL3014.2
002300 FILE SECTION.                                                    RL3014.2
002400 FD  TFIL.                                                        RL3014.2
002500 01  FREC.                                                        RL3014.2
002600     03 GUBBINS  PIC 9(8).                                        RL3014.2
002700                                                                  RL3014.2
002800 WORKING-STORAGE SECTION.                                         RL3014.2
002900 01  VARIABLES.                                                   RL3014.2
003000   03  STATE     PIC X(4) VALUE SPACES.                           RL3014.2
003100   03  RKEY      PIC 9(8) VALUE ZERO.                             RL3014.2
003200                                                                  RL3014.2
003300 PROCEDURE DIVISION.                                              RL3014.2
003400                                                                  RL3014.2
003500 RL301M-CONTROL.                                                  RL3014.2
003600     OPEN I-O TFIL.                                               RL3014.2
003700     PERFORM RL301M-READ THRU RL301M-DELETE 1 TIMES.              RL3014.2
003800     CLOSE TFIL.                                                  RL3014.2
003900     STOP RUN.                                                    RL3014.2
004000                                                                  RL3014.2
004100 RL301M-READ.                                                     RL3014.2
004200     READ TFIL INVALID KEY PERFORM INV-PARA                       RL3014.2
004300                 NOT INVALID KEY PERFORM DONE-PARA.               RL3014.2
004400*Message expected for above statement: NON-CONFORMING STANDARD    RL3014.2
004500                                                                  RL3014.2
004600 RL301M-REWRITE.                                                  RL3014.2
004700     REWRITE FREC INVALID KEY PERFORM INV-PARA                    RL3014.2
004800                 NOT INVALID KEY PERFORM DONE-PARA.               RL3014.2
004900*Message expected for above statement: NON-CONFORMING STANDARD    RL3014.2
005000                                                                  RL3014.2
005100 RL301M-WRITE.                                                    RL3014.2
005200     WRITE FREC INVALID KEY PERFORM INV-PARA                      RL3014.2
005300                 NOT INVALID KEY PERFORM DONE-PARA.               RL3014.2
005400*Message expected for above statement: NON-CONFORMING STANDARD    RL3014.2
005500                                                                  RL3014.2
005600 RL301M-DELETE.                                                   RL3014.2
005700     DELETE TFIL INVALID KEY PERFORM INV-PARA                     RL3014.2
005800                 NOT INVALID KEY PERFORM DONE-PARA.               RL3014.2
005900*Message expected for above statement: NON-CONFORMING STANDARD    RL3014.2
006000                                                                  RL3014.2
006100 INV-PARA.                                                        RL3014.2
006200     MOVE "INVA" TO STATE.                                        RL3014.2
006300                                                                  RL3014.2
006400 DONE-PARA.                                                       RL3014.2
006500     MOVE "DONE" TO STATE.                                        RL3014.2
006600                                                                  RL3014.2
006700*TOTAL NUMBER OF FLAGS EXPECTED = 6.                              RL3014.2
      *END-OF,RL301M                                                                  
