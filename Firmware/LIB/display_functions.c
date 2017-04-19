
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include "display.h"
#include "uart.h"
#include "general_functions.h"
#include "utils/uartstdio.h"

void display(char* type, int range, int value, int decimal){
    // convert 123 to string [buf]
    char value_buf[10];
    char range_buf[10];
    char decimal_buf[10];
    //UARTprintf("Attempting to convert: %d\n", value);
    //char buffer[10];

    int set_negative = 0;

    if(value < 0){
        value = -1 * value;
        set_negative = 1;

    }
    int2str(value, value_buf, 10);
    int2str(decimal, decimal_buf, 10);
    int2str(range, range_buf, 10);

    //UARTprintf("Converted to: %s", buf);
    if(strcmp(type, "voltage") == 0){
      //UARTprintf("dec: %s\n", decimal_buf);//same type, outputting to display\n");
      clearLCD();

  		printLCD("Voltage ( ");
  		sendByte(0x00, TRUE);
  		printLCD(range_buf);
  		printLCD("V )");
  		setCursorPositionLCD(1,0);
  		printLCD("V: ");

  		if(set_negative == 1){
  		    printLCD("-");
  		}

  		printLCD(value_buf);
  		printLCD(".");
  		printLCD(decimal_buf);
  		printLCD("V");
    }
    else if(strcmp(type, "current") == 0){
      clearLCD();
      printLCD("Current (");
      sendByte(0x00, TRUE);
      printLCD(range_buf);
      printLCD("mA)");
      setCursorPositionLCD(1,0);
      printLCD("C: ");
      if(set_negative == 1){
          printLCD("-");
      }
      printLCD(value_buf);
      printLCD(".");
      printLCD(decimal_buf);
      printLCD("mA");
      }
    else{
        UARTprintf("No Match!\n");
    }

    return;
}
