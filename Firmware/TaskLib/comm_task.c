//*****************************************************************************
//
// comm_task.c - A simple flashing LED task.
//
// Copyright (c) 2012-2014 Texas Instruments Incorporated.  All rights reserved.
// Software License Agreement
//
// Texas Instruments (TI) is supplying this software for use solely and
// exclusively on TI's microcontroller products. The software is owned by
// TI and/or its suppliers, and is protected under applicable copyright
// laws. You may not combine this software with "viral" open-source
// software in order to form a larger program.
//
// THIS SOFTWARE IS PROVIDED "AS IS" AND WITH ALL FAULTS.
// NO WARRANTIES, WHETHER EXPRESS, IMPLIED OR STATUTORY, INCLUDING, BUT
// NOT LIMITED TO, IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
// A PARTICULAR PURPOSE APPLY TO THIS SOFTWARE. TI SHALL NOT, UNDER ANY
// CIRCUMSTANCES, BE LIABLE FOR SPECIAL, INCIDENTAL, OR CONSEQUENTIAL
// DAMAGES, FOR ANY REASON WHATSOEVER.
//
// This is part of revision 2.1.0.12573 of the EK-TM4C123GXL Firmware Package.
//
//*****************************************************************************

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

#include "lcd_task.h"
#include "ADC_task.h"

//LCD INCLUDES

#include "driverlib/rom.h"
#include "utils/uartstdio.h"
#include "driverlib/pin_map.h"
#include "mswitch_task.h"
#include "switch_task.h"

//*****************************************************************************
//
// The stack size for the LED toggle task.
//
//*****************************************************************************
#define COMMTASKSTACKSIZE        128         // Stack size in words

//*****************************************************************************
//
// The item size and queue size for the LED message queue.
//
//*****************************************************************************
#define COMM_ITEM_SIZE           sizeof(uint8_t)
#define COMM_QUEUE_SIZE          5

#define COMM_REFRESH_TIME 100
//*****************************************************************************
//
// The queue that holds messages sent to the LED task.
//
//*****************************************************************************
xQueueHandle g_pCOMMQueue;

extern xSemaphoreHandle g_pUARTSemaphore;


static void
CommTask(void *pvParameters)
{
    portTickType ui32WakeTime;
    uint32_t ui32COMMRefreshTime;
    //uint8_t i8Message;

    //
    // Initialize the LED Toggle Delay to default value.
    //
    ui32COMMRefreshTime = COMM_REFRESH_TIME;

    struct mswitch_queue_message mswitch_message;
    struct lcd_queue_message lcd_message;
    struct adc_queue_message adc_message;
    struct switch_queue_message switch_message;

    //
    // Get the current tick count.
    //
    ui32WakeTime = xTaskGetTickCount();

    int received_valid = 0;

    char buffer[64];
    //
    // Loop forever.
    //
    while(1)
    {
      //
      // Read the next message, if available on queue.
      //
      /*if(xQueueReceive(g_pLCDQueue, &i8Message, 0) == pdPASS)
      {

      }*/

      //UARTprintf("Getting UART...\n\r");
      //display("voltage",0, 0, 0);
      memset(buffer, 0, sizeof(buffer));
      UARTgets(buffer, sizeof(buffer));

      /*xSemaphoreTake(g_pUARTSemaphore, portMAX_DELAY);
      UARTprintf("{%s}\n\r", buffer);
      UARTprintf("RECIEVED CHARACTERS: ");
      for(int i = 0; i < sizeof(buffer); i++){
        UARTprintf("%c ", buffer[i]);
      }
      UARTprintf("\n\r");

      UARTprintf("RECIEVED CHARACTERS DIGITS: ");
      for(int i = 0; i < sizeof(buffer); i++){
        UARTprintf("%d ", buffer[i]);
      }
      UARTprintf("\n\r");
      xSemaphoreGive(g_pUARTSemaphore);*/



      mswitch_message.value = 0; //doesn't matter
      received_valid = 0;

      if(buffer[1] == 'C'){
        if( xSemaphoreTake(g_pUARTSemaphore,portMAX_DELAY) == pdTRUE )
        {
          UARTprintf("[C]\n\r");
          xSemaphoreGive(g_pUARTSemaphore);
        }
      } else if(buffer[0] == 'S'){
        if(buffer[2] == 'M'){
          mswitch_message.type = 'M'; //sending M for mode
          if(buffer[4] == 'V'){
            mswitch_message.mode = 'V'; //sending V to for voltage
            received_valid = 1;
          } else if(buffer[4] == 'I'){
            mswitch_message.mode = 'I'; //sending I to for current
            received_valid = 1;
          } else if(buffer[4] == 'R'){
            mswitch_message.mode = 'R'; //sending R to for resistance
            received_valid = 1;
          } else if(buffer[4] == 'C'){
            mswitch_message.mode = 'C'; //sending C to for resistance
            received_valid = 1;
          } else if(buffer[4] == 'L'){
            mswitch_message.mode = 'L'; //sending L to for resistance
            received_valid = 1;
          } else {
            UARTprintf("{UNKNOWN MODE RECEIVED}\n\r");
          }
        } else if (buffer[0] == 'F'){ //Recieved Frequency
          adc_message.mode = 'F';
          if(buffer[2] == 'A'){
            adc_message.frequency = 500;
            received_valid = 1;
          } else if(buffer[2] == 'B'){
            adc_message.frequency = 1000;
            received_valid = 1;
          } else if(buffer[2] == 'C'){
            adc_message.frequency = 2000;
            received_valid = 1;
          } else if(buffer[2] == 'D'){
            adc_message.frequency = 5000;
            received_valid = 1;
          } else if(buffer[2] == 'E'){
            adc_message.frequency = 10000;
            received_valid = 1;
          } else if(buffer[2] == 'F'){
            adc_message.frequency = 60000;
            received_valid = 1;
          } else if(buffer[2] == 'G'){
            adc_message.frequency = 120000;
            received_valid = 1;
          } else if(buffer[2] == 'H'){
            adc_message.frequency = 300000;
            received_valid = 1;
          } else if(buffer[2] == 'I'){
            adc_message.frequency = 600000;
            received_valid = 1;
          } else{
            UARTprintf("{UNKNOWN SETTING RECEIVED}\n\r");
          }
          if(received_valid == 1){
            if(xQueueSend(g_pADCQueue, &adc_message, portMAX_DELAY) !=
               pdPASS){
                 UARTprintf("FAILED TO SEND TO MSWITCH QUEUE\n\r");
               }
          }
        }

        if(received_valid){
          if(xQueueSend(g_pMSWITCHQueue, &mswitch_message, portMAX_DELAY) !=
             pdPASS){
               UARTprintf("FAILED TO SEND TO MSWITCH QUEUE\n\r");
             }
        }

      } else if (buffer[0] == 'B'){
         lcd_message.setting = 1;
         if(buffer[2] == '4'){
           lcd_message.brightness = 4;
         } else if(buffer[2] == '3'){
           lcd_message.brightness = 3;
         } else if(buffer[2] == '2'){
           lcd_message.brightness = 2;
         } else if(buffer[2] == '1'){
           lcd_message.brightness = 1;
         } else if(buffer[2] == '0'){
           lcd_message.brightness = 0;
         }
         //Send to LCD Queue
         if(xQueueSend(g_pLCDQueue, &lcd_message, portMAX_DELAY) !=
            pdPASS){
              UARTprintf("FAILED TO SEND TO MSWITCH QUEUE\n\r");
          }
      } else if (buffer[0] == 'H'){

        //switch_message.power = 1;
         //Send to LCD Queue
         if(xQueueSend(g_pSWITCHQueue, &switch_message, portMAX_DELAY) !=
            pdPASS){
              UARTprintf("FAILED TO SEND TO MSWITCH QUEUE\n\r");
          }
          switch_message.power = 0;
      } else{
          UARTprintf("{UNKNOWN COMMAND RECEIVED}\n\r");
          UARTprintf("RECIEVED: %s\n\r", buffer);
      }


      //
      // Wait for the required amount of time.
      //
      vTaskDelayUntil(&ui32WakeTime, ui32COMMRefreshTime / portTICK_RATE_MS);
    }
}

//*****************************************************************************
//
// Initializes the LED task.
//
//*****************************************************************************
uint32_t
CommTaskInit(void)
{
    //
    // Create a queue for sending messages to the LED task.
    //
    g_pCOMMQueue = xQueueCreate(COMM_QUEUE_SIZE, COMM_ITEM_SIZE);

    //initLCD();
    //
    // Create the LED task.
    //
    if(xTaskCreate(CommTask, (signed portCHAR *)"COMM", COMMTASKSTACKSIZE, NULL,
                   tskIDLE_PRIORITY + PRIORITY_COMM_TASK, NULL) != pdTRUE)
    {
        return(1);
    }

    UARTprintf("    Comms initiated.\n\r");
    //
    // Success.
    //
    return(0);
}
