#ifndef SD_CARD_H_   /* Include guard */
#define SD_CARD_H_

//Configure UART
extern int check_filename(char * filename);
extern void initialise_sd_card(void);
extern int append_to_file(char * filename, char * text);


#endif // UART_H_
