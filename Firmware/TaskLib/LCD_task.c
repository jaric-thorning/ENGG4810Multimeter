#include <stdbool.h>
#include <stdint.h>
#include <string.h>
#include "inc/hw_memmap.h"
#include "inc/hw_types.h"
#include "driverlib/gpio.h"
#include "driverlib/rom.h"
#include "drivers/rgb.h"
#include "drivers/buttons.h"
#include "utils/uartstdio.h"
#include "led_task.h"
#include "priorities.h"
#include "FreeRTOS.h"
#include "task.h"
#include "queue.h"
#include "semphr.h"

#include "bget.h"


#include "LCD_task.h"

#include "stdlib.h"
//LCD INCLUDES

#include "display_functions.h"
#include "driverlib/rom.h"
#include "utils/uartstdio.h"
#include "driverlib/pin_map.h"
#include "driverlib/sysctl.h"
#include "driverlib/systick.h"

#include "display.h"

#include "driverlib/pwm.h"

#include "general_functions.h"

#include "menu.h"


#define LCDTASKSTACKSIZE        128

#define LCD_ITEM_SIZE           sizeof(struct lcd_queue_message)
#define LCD_QUEUE_SIZE          5

#define LCD_REFRESH_TIME 10

extern xSemaphoreHandle g_pUARTSemaphore;

//void format_read_value(char type, int range, int value, int decimal, char * line1, char * line2);


void update_display(int line, char * text){

  setCursorPositionLCD(line,0);
  int num_chars = 0;

  char *c;
	c = text;
	while ((c != 0) && (*c != 0))
	{
		c++;
    num_chars++;
	}

  //UARTprintf("%d : %s\n\r", num_chars, text);

  if(num_chars > 16){
    num_chars = 16;
  }
  for(int i = 0; i < num_chars; i++){
    if(text[i] == '='){
      sendByte(0x00, TRUE);
    } else if(text[i] == ';'){
      sendByte(0x01, TRUE);
    }
    else{
      sendByte(text[i], TRUE);
    }
  }
  for(int i = num_chars; i < 16; i++){
    sendByte(' ', TRUE);
  }
}

/*void shift_left(char * screen, int row){
  char prev_char;
  for(int i = 0; i < (sizeof(screen) - 1); i++){
    prev_char = screen[i];
    screen[i] = screen[i + 1];
  }
  screen[sizeof(screen)] =
}*/

void format_read_value(char type, int range, int value, int decimal, char ** line1, char ** line2){
  char value_buf[10];
  char range_buf[10];
  char decimal_buf[10];
  static char build1[16];
  static char build2[16];
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

  if(type == 'V'){
    strcpy(build1, "Voltage (");
    strcpy(build2, "V: ");
  } else if(type == 'I'){
    strcpy(build1, "Current (");
    strcpy(build2, "I: ");
  } else if(type == 'R'){
    strcpy(build1, "Res (");
    strcpy(build2, "R: ");
  } else if(type == 'C'){
    strcpy(build1, "Cont (");
    strcpy(build2, "C: ");
  } else if (type == 'L'){
    strcpy(build1, "Logic (");
    strcpy(build2, "L: ");
  } else {
    strcpy(build1, "Unknown");
    strcpy(build2, "U: ");
  }
  strcat(build1, "=");
  strcat(build1, range_buf);
  strcat(build1, ")");

  if(set_negative){
    strcat(build2, "-");
  }
  strcat(build2, value_buf);
  strcat(build2, ".");
  strcat(build2, decimal_buf);

  if(type == 'V'){
    strcat(build2, "V");
  } else if(type == 'I'){
    strcat(build2, "mA");
  } else if(type == 'R'){
    strcat(build2, ";");
  } else if(type == 'C'){
    strcat(build2, "C");
  } else if (type == 'L'){
    strcat(build2, "L");
  } else {
    strcat(build2, "U");
  }



  strcat(build1, "\0");
  strcat(build2, "\0");

  *line1 = build1;
  *line2 = build2;

  return;
}

static void
LCDTask(void *pvParameters)
{
    portTickType ui32WakeTime;
    uint32_t ui32LCDRefreshTime;
    struct lcd_queue_message lcd_message;

    ui32LCDRefreshTime = LCD_REFRESH_TIME;

    ui32WakeTime = xTaskGetTickCount();

    //top_line = (char*)bgetz(16 * sizeof(char));
    //bottom_line = (char*)bgetz(16 * sizeof(char));
    unsigned long period = 5000;

    char* lcd_line_1 = bgetz(16 * sizeof(char));
    char* lcd_line_2 = bgetz(16 * sizeof(char));

    /*
    char current_title[] = "Main Menu       ";
    char next_title[] = "Frequency       ";


    char current_item[] = "Frequency       ";
    char next_item[] = "                ";*/

    clearLCD();

    char mode = 'D'; //D -> display, M -> Menu

    struct Menu menu;

    init_menu(&menu);

    int item = 0;
    int selection = 0;
    while(1)
    {
      //
      // Read the next message, if available on queue.
      //
      if(xQueueReceive(g_pLCDQueue, &lcd_message, 0) == pdPASS){
        xSemaphoreTake(g_pUARTSemaphore, portMAX_DELAY);
        if(lcd_message.setting == 1){
          PWMPulseWidthSet(PWM0_BASE, PWM_OUT_3, period * lcd_message.brightness/5);;
          PWMOutputState(PWM0_BASE, PWM_OUT_3_BIT, lcd_message.brightness);
          if(lcd_message.brightness == 0){
            GPIOPinWrite(GPIO_PORTE_BASE, GPIO_PIN_1, GPIO_PIN_1);
          }
          else{
            GPIOPinWrite(GPIO_PORTE_BASE, GPIO_PIN_1, 0);
          }
        }

        if(lcd_message.mode == 'M'){
          //Change to menu mode, can only return to display mode from menu
          mode = 'M';
          if(lcd_message.button == 'S'){
            UARTprintf("Selection.\n\r");

            if(menu.active){
              //do selections
                // if at main menu, select sub menu

                // if at sub menu, select option
              menu.selection = (menu.selection + 1) % 2;
              UARTprintf("Changing Menu\n\r");
              menu.scroll_title = 1;
              menu.new_title = "Frequency       ";
              menu.scroll_item = 1;
              menu.new_item = get_text(menu.selection, item);

            } else{
              UARTprintf("Launching Menu\n\r");
              launch_menu(&menu);
              menu.new_title = "Main Menu       ";
              menu.new_item =  "Frequency       ";
            }
          } else if(lcd_message.button == 'N'){
            UARTprintf("Next.\n\r");
            menu.scroll_item = 1;
            menu.scroll_item = 1;
            menu.new_item = menu.items[0][0];
          } else if(lcd_message.button == 'B'){
            UARTprintf("Back.\n\r");
          }
        }

        if(mode == 'D'){
          UARTprintf("[%c: %d.%d]\n\r", lcd_message.type, lcd_message.value, lcd_message.decimal);
          //displayOffLCD();

          format_read_value(lcd_message.type, lcd_message.range, lcd_message.value, lcd_message.decimal, &lcd_line_1, &lcd_line_2);

          //UARTprintf("A1: %s\n\r", lcd_line_1);
          //UARTprintf("A2: %s\n\r", lcd_line_2);

          UARTprintf("[D1 %s]\n\r", lcd_line_1);
          UARTprintf("[D2 %s]\n\r", lcd_line_2);
        }
        xSemaphoreGive(g_pUARTSemaphore);
      }

      if(mode == 'M'){
        if(menu.active){
          if(update_title(&menu)){
            update_item(&menu);
          };

          format_menu(&menu, &lcd_line_1, &lcd_line_2);
        }
      }
      update_display(0, lcd_line_1);
      update_display(1, lcd_line_2);

      //
      // Wait for the required amount of time.
      //
      vTaskDelayUntil(&ui32WakeTime, ui32LCDRefreshTime / portTICK_RATE_MS);
    }
}
uint32_t
LCDTaskInit(void)
{
    g_pLCDQueue = xQueueCreate(LCD_QUEUE_SIZE, LCD_ITEM_SIZE);

    initLCD();

    // Enable the peripherals used by this program.
    SysCtlPeripheralEnable(SYSCTL_PERIPH_GPIOB);
    SysCtlPeripheralEnable(SYSCTL_PERIPH_PWM0);  //The Tiva Launchpad has two modules (0 and 1).
    SysCtlPeripheralEnable(SYSCTL_PERIPH_GPIOE);
    while(!SysCtlPeripheralReady(SYSCTL_PERIPH_GPIOB))
    {
    }

    while(!SysCtlPeripheralReady(SYSCTL_PERIPH_PWM0))
    {
    }
    while(!SysCtlPeripheralReady(SYSCTL_PERIPH_GPIOE))
    {
    }

    GPIOPinTypeGPIOOutput(GPIO_PORTE_BASE, GPIO_PIN_1);
    GPIOPinWrite(GPIO_PORTE_BASE, GPIO_PIN_1, 0);

    GPIOPinConfigure(GPIO_PB5_M0PWM3);
    GPIOPinTypePWM(GPIO_PORTB_BASE, GPIO_PIN_5);


    //Configure PWM Options
    //PWM_GEN_2 Covers M1PWM4 and M1PWM5
    //PWM_GEN_3 Covers M1PWM6 and M1PWM7 See page 207 4/11/13 DriverLib doc
    PWMGenConfigure(PWM0_BASE, PWM_GEN_1, PWM_GEN_MODE_DOWN | PWM_GEN_MODE_NO_SYNC);

    unsigned long period = 5000;
    unsigned long pwmNow = period/10.0;

    //Set the Period (expressed in clock ticks)
    PWMGenPeriodSet(PWM0_BASE, PWM_GEN_1, period);

    //Set PWM duty-50% (Period /2)
    PWMPulseWidthSet(PWM0_BASE, PWM_OUT_3,period);

    // Enable the PWM generator
    PWMGenEnable(PWM0_BASE, PWM_GEN_1);

    // Turn on the Output pins
    //PWMOutputState(PWM0_BASE, PWM_OUT_0_BIT | PWM_OUT_2_BIT | PWM_OUT_4_BIT | PWM_OUT_5_BIT | PWM_OUT_6_BIT | PWM_OUT_7_BIT, true);
    PWMOutputState(PWM0_BASE, PWM_OUT_3_BIT, true);

    printLCD("Starting LCD...");

    if(xTaskCreate(LCDTask, (signed portCHAR *)"LCD", LCDTASKSTACKSIZE, NULL,
                   tskIDLE_PRIORITY + PRIORITY_LCD_TASK, NULL) != pdTRUE)
    {
        return(1);
    }

    UARTprintf("LCD initiated...\n\r");

    //
    // Success.
    //
    return(0);
}
