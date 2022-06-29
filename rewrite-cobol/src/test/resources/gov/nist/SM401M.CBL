      *HEADER,COBOL,SM401M                                                            
000100 IDENTIFICATION DIVISION.                                         SM4014.2
000200 PROGRAM-ID.                                                      SM4014.2
000300     SM401M.                                                      SM4014.2
000400*THE FOLLOWING PROGRAM TESTS THE FLAGGING OF HIGH                 SM4014.2
000500*SUBSET FEATURES THAT ARE USED IN SOURCE TEXT                     SM4014.2
000600*MANIPULATION.                                                    SM4014.2
000700 ENVIRONMENT DIVISION.                                            SM4014.2
000800 CONFIGURATION SECTION.                                           SM4014.2
000900 SOURCE-COMPUTER.                                                 SM4014.2
001000     XXXXX082.                                                    SM4014.2
001100 OBJECT-COMPUTER.                                                 SM4014.2
001200     XXXXX083.                                                    SM4014.2
001300                                                                  SM4014.2
001400                                                                  SM4014.2
001500 DATA DIVISION.                                                   SM4014.2
001600 PROCEDURE DIVISION.                                              SM4014.2
001700                                                                  SM4014.2
001800 SM401M-CONTROL.                                                  SM4014.2
001900     PERFORM SM401M-COPYREP THRU SM401M-REPL.                     SM4014.2
002000     STOP RUN.                                                    SM4014.2
002100                                                                  SM4014.2
002200 SM401M-COPYREP.                                                  SM4014.2
002300*Message expected for following statement: NON-CONFORMING STANDARDSM3014.2
002400     COPY KSM41 REPLACING "PIG" BY "HORSE".                       SM4014.2
002500                                                                  SM4014.2
002600 SM401M-REPL.                                                     SM4014.2
002700     REPLACE OFF.                                                 SM4014.2
002800*Message expected for above statement: NON-CONFORMING STANDARD    SM4014.2
002900                                                                  SM4014.2
003000*TOTAL NUMBER OF FLAGS EXPECTED = 2.                              SM4014.2
      *END-OF,SM401M                                                                  
