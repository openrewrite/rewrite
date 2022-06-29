      *HEADER,CLBRY,KP004                                                             
000100*    THIS COMMENT IS THE FIRST IMAGE IN KP004                     KP0044.2
000200*    ADD  1 TO THE LIST.                                          KP0044.2
000300 PST-INIT-004.                                                    KP0044.2
000400     MOVE "PSEUDO-TEXT/WORD" TO FEATURE.                          KP0044.2
000500     MOVE    ZERO TO WRK-DS-09V00-901.                            KP0044.2
000600     MOVE    "PST-TEST-004" TO PAR-NAME.                          KP0044.2
000700 PST-TEST-004.                                                    KP0044.2
000800     ADD     5 TO WRK-DS-09V00-901.                               KP0044.2
000900     THIS IS NOT REAL COBOL-74 SYNTAX HOWEVER                     KP0044.2
001000             SHOVE +2 TO WRK-DS-09V00-902.                        KP0044.2
001100     GO TO   PST-EXIT-004.                                        KP0044.2
001200 PST-DELETE-004.                                                  KP0044.2
001300     PERFORM DELETE.                                              KP0044.2
001400 PST-EXIT-004.                                                    KP0044.2
001500     EXIT.                                                        KP0044.2
      *END-OF,KP004                                                                   
