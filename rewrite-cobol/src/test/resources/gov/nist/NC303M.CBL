      *HEADER,COBOL,NC303M                                                            
000100 IDENTIFICATION DIVISION.                                         NC3034.2
000200 PROGRAM-ID.                                                      NC3034.2
000300     NC303M.                                                      NC3034.2
000400*THE FOLLOWING PROGRAM TESTS THE FLAGGING OF                      NC3034.2
000500*OBSOLETE HIGH SUBSET NUCLEUS FEATURES.                           NC3034.2
000600 DATE-COMPILED.  22ND AUG 1988.                                   NC3034.2
000700*Message expected for above statement: OBSOLETE                   NC3034.2
000800 ENVIRONMENT DIVISION.                                            NC3034.2
000900 CONFIGURATION SECTION.                                           NC3034.2
001000 SOURCE-COMPUTER.                                                 NC3034.2
001100     XXXXX082.                                                    NC3034.2
001200 OBJECT-COMPUTER.                                                 NC3034.2
001300     XXXXX083.                                                    NC3034.2
001400                                                                  NC3034.2
001500                                                                  NC3034.2
001600 PROCEDURE DIVISION.                                              NC3034.2
001700                                                                  NC3034.2
001800 NC303M-CONTROL.                                                  NC3034.2
001900     ALTER NC303M-GOTO TO PROCEED TO NC303M-GOTO-2,               NC3034.2
002000         NC303M-GOTO-2 TO PROCEED TO NC303M-CONTROL.              NC3034.2
002100*Message expected for above statement: OBSOLETE                   NC3034.2
002200     STOP RUN.                                                    NC3034.2
002300                                                                  NC3034.2
002400 NC303M-GOTO.                                                     NC3034.2
002500     GO TO.                                                       NC3034.2
002600*Message expected for above statement: OBSOLETE                   NC3034.2
002700                                                                  NC3034.2
002800 NC303M-GOTO-2.                                                   NC3034.2
002900     GO TO.                                                       NC3034.2
003000*Message expected for above statement: OBSOLETE                   NC3034.2
003100                                                                  NC3034.2
003200*TOTAL NUMBER OF FLAGS EXPECTED = 4.                              NC3034.2
      *END-OF,NC303M                                                                  
