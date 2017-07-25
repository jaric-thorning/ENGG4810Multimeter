/*
 * Helper functions for M_SWITCH
 */
#include <stdbool.h>
#include <stdint.h>
#include <string.h>

#include "FreeRTOS.h"
#include "task.h"
#include "queue.h"
#include "semphr.h"

#include "utils/uartstdio.h"
#include "sd_task.h"
#include "sd_card.h"
#include "switch_task.h"
#include "general_functions.h"

#define DC_VOLTAGE 0
#define AC_VOLTAGE 1
#define DC_CURRENT 2
#define AC_CURRENT 3
#define RESISTANCE 4
#define LOGIC      5
#define CONTINUITY 6

#define NUM_MODES 7

#define DISABLE_AC 0

char temp_filename[64];
static char header_text[64] = "Time,Value,Units,IsoTime\n\0";
char sd_write_line[64];


 /*
  * Function to update mode
  */

extern int next_mode(int current_mode, char requested_update){
  int mode = DC_VOLTAGE;

  if(requested_update == 'V'){
    mode = DC_VOLTAGE;
  } else if(requested_update == 'W'){
    mode = AC_VOLTAGE;
  } else if(requested_update == 'I'){
    mode = DC_CURRENT;
  } else if(requested_update == 'J'){
    mode = AC_CURRENT;
  } else if(requested_update == 'R'){
    mode = RESISTANCE;
  } else if(requested_update == 'C'){
    mode = CONTINUITY;
  } else if(requested_update == 'L'){
    mode = LOGIC;
  } else if(requested_update == 'U'){
    mode = (current_mode + 1) % NUM_MODES;
  }
  else if(requested_update == 'D'){
    mode = (current_mode - 1) % NUM_MODES;
  } else{
    UARTprintf("WARNING - ATTEMPTED TO SWITCH TO UNKNOWN MODE\n\r");
  }
  /*Disable AC_MODES*/
  if(DISABLE_AC){
    if(mode == AC_VOLTAGE || mode == AC_CURRENT){
      mode++;
    }
  }
  return mode;
}

/*
 * Triggers SD card logging, returns logging state
 */
extern int trigger_sd_logging(int logging, struct sd_queue_message * sd_message, int filename_count){
  int updated_logging = logging;
  if(logging == 0){
    UARTprintf("Recording to SD....\n\r");
    updated_logging = 1;

    //char temp_filename[64];
    char temp_type[2];
    char counter_buf[10];

    do {
      strcpy(temp_filename,"logfile\0");
      int2str(filename_count++, counter_buf, 10);
      strcat(temp_filename, counter_buf);
      strcat(temp_filename, ".csv\0");

    } while(check_filename(temp_filename));

    sd_message->filename = temp_filename;
    sd_message->text = header_text;
    UARTprintf("Found valid filname: %s\n\r", sd_message->filename);

    if(xQueueSend(g_pSDQueue, sd_message, portMAX_DELAY) !=
           pdPASS){
             UARTprintf("FAILED TO SEND TO LCD QUEUE\n\r");
           }

  } else{
    UARTprintf("Recording done.\n\r");
    updated_logging = 0;
  }
  return updated_logging;
}

extern void record_to_sd(int logging, double integer, double decimal,
  char current_mode_char, struct sd_queue_message * sd_message){

  if(logging){
    char integer_buf[10];
    char decimal_buf[10];

    memset(decimal_buf, 0, 10);
    memset(integer_buf, 0, 10);
    memset(sd_write_line, 0, 64);
    //memset(sd_message.text, 0, sizeof(sd_message.text));
    //sd_message.text = "TEST";
    int ticks_seconds = xTaskGetTickCount();
    int2str(ticks_seconds / 1000, integer_buf, 10);
    int2str(ticks_seconds % 1000, decimal_buf, 10);
    strncpy(sd_write_line, integer_buf, 10);
    strcat(sd_write_line, ".");
    strcat(sd_write_line, decimal_buf);
    strcat(sd_write_line, ",");
    if(integer < 0){
      integer *= -1;
      strcat(sd_write_line, "-");
    }
    int2str(integer, integer_buf, 10);
    int2str(decimal, decimal_buf, 10);
    strcat(sd_write_line, integer_buf);
    strcat(sd_write_line, ".");
    strcat(sd_write_line, decimal_buf);
    strcat(sd_write_line, ",");
    if(current_mode_char == 'V'){
      strcat(sd_write_line, "V,");
    } else if (current_mode_char == 'C'){
      strcat(sd_write_line, "C,");
    } else if (current_mode_char == 'R'){
      strcat(sd_write_line, "R,");
    } else{
      strcat(sd_write_line, "U,");
    }
    strcat(sd_write_line, "N\n");

    //sd_message->filename = "help.csv";
    sd_message->text = sd_write_line;

    //UARTprintf("Current filename : %s\n\r", sd_message->filename);

    if(xQueueSend(g_pSDQueue, sd_message, portMAX_DELAY) !=
       pdPASS){
         UARTprintf("FAILED TO SEND TO LCD QUEUE\n\r");
       }
    }
}
