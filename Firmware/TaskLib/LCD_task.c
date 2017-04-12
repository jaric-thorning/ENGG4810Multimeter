//*****************************************************************************
//
// led_task.c - A simple flashing LED task.
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


//LCD INCLUDES

#include "LIB/display_functions.h"
#include "driverlib/rom.h"
#include "utils/uartstdio.h"
#include "driverlib/pin_map.h"


//*****************************************************************************
//
// The stack size for the LED toggle task.
//
//*****************************************************************************
#define LCDTASKSTACKSIZE        128         // Stack size in words

//*****************************************************************************
//
// The item size and queue size for the LED message queue.
//
//*****************************************************************************
#define LCD_ITEM_SIZE           sizeof(uint8_t)
#define LCD_QUEUE_SIZE          5



#define LCD_REFRESH_TIME 500
//*****************************************************************************
//
// The queue that holds messages sent to the LED task.
//
//*****************************************************************************
xQueueHandle g_pLCDQueue;

extern xSemaphoreHandle g_pUARTSemaphore;


static void
LCDTask(void *pvParameters)
{
    portTickType ui32WakeTime;
    uint32_t ui32LCDRefreshTime;
    uint8_t i8Message;

    //
    // Initialize the LED Toggle Delay to default value.
    //
    ui32LCDRefreshTime = LCD_REFRESH_TIME;

    //
    // Get the current tick count.
    //
    ui32WakeTime = xTaskGetTickCount();

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

      display("voltage",0, 0, 0);
      //
      // Wait for the required amount of time.
      //
      vTaskDelayUntil(&ui32WakeTime, ui32LCDRefreshTime / portTICK_RATE_MS);
    }
}

//*****************************************************************************
//
// Initializes the LED task.
//
//*****************************************************************************
uint32_t
LCDTaskInit(void)
{
    //
    // Create a queue for sending messages to the LED task.
    //
    g_pLCDQueue = xQueueCreate(LCD_QUEUE_SIZE, LCD_ITEM_SIZE);

    initLCD();
    //
    // Create the LED task.
    //
    if(xTaskCreate(LCDTask, (signed portCHAR *)"LCD", LCDTASKSTACKSIZE, NULL,
                   tskIDLE_PRIORITY + PRIORITY_LCD_TASK, NULL) != pdTRUE)
    {
        return(1);
    }

    //
    // Success.
    //
    return(0);
}
