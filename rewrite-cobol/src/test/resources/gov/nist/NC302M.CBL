      *HEADER,COBOL,NC302M                                                            
000100 IDENTIFICATION DIVISION.                                         NC3024.2
000200 PROGRAM-ID.                                                      NC3024.2
000300     NC302M.                                                      NC3024.2
000400*THE FOLLOWING PROGRAM TESTS THE FLAGGING OF OBSOLETE             NC3024.2
000500*MINIMUM SUBSET NUCLEUS FEATURES.                                 NC3024.2
000600 AUTHOR. DAVID G BAMBER.                                          NC3024.2
000700*Message expected for above statement: OBSOLETE                   NC3024.2
000800 INSTALLATION. NCC.                                               NC3024.2
000900*Message expected for above statement: OBSOLETE                   NC3024.2
001000 DATE-WRITTEN. 19TH AUG 1988.                                     NC3024.2
001100*Message expected for above statement: OBSOLETE                   NC3024.2
001200 SECURITY. NO SECURITY.                                           NC3024.2
001300*Message expected for above statement: OBSOLETE                   NC3024.2
001400 ENVIRONMENT DIVISION.                                            NC3024.2
001500 CONFIGURATION SECTION.                                           NC3024.2
001600 SOURCE-COMPUTER.                                                 NC3024.2
001700     XXXXX082.                                                    NC3024.2
001800 OBJECT-COMPUTER.                                                 NC3024.2
001900     XXXXX083                                                     NC3024.2
002000     MEMORY SIZE                                                  NC3024.2
002100     XXXXX068                                                     NC3024.2
002200     CHARACTERS.                                                  NC3024.2
002300*Message expected for above statement: OBSOLETE                   NC3024.2
002400                                                                  NC3024.2
002500                                                                  NC3024.2
002600 DATA DIVISION.                                                   NC3024.2
002700 PROCEDURE DIVISION.                                              NC3024.2
002800                                                                  NC3024.2
002900 NC302M-CONTROL.                                                  NC3024.2
003000     PERFORM NC302M-ALTER THRU NC302M-STOP.                       NC3024.2
003100     STOP RUN.                                                    NC3024.2
003200                                                                  NC3024.2
003300 NC302M-ALTER.                                                    NC3024.2
003400     ALTER NC302M-PROC1 TO NC302M-PROC2.                          NC3024.2
003500*Message expected for above statement: OBSOLETE                   NC3024.2
003600                                                                  NC3024.2
003700 NC302M-PROC1.                                                    NC3024.2
003800     GO TO NC302M-PROC2.                                          NC3024.2
003900                                                                  NC3024.2
004000 NC302M-PROC2.                                                    NC3024.2
004100     DISPLAY "DUMMY PROCEDURE".                                   NC3024.2
004200                                                                  NC3024.2
004300                                                                  NC3024.2
004400                                                                  NC3024.2
004500                                                                  NC3024.2
004600 NC302M-STOP.                                                     NC3024.2
004700     STOP "FNC302".                                               NC3024.2
004800*Message expected for above statement: OBSOLETE                   NC3024.2
004900                                                                  NC3024.2
005000                                                                  NC3024.2
005100*TOTAL NUMBER OF FLAGS EXPECTED = 7.                              NC3024.2
      *END-OF,NC302M                                                                  
