//*****************************************************************************
//
// freertos_demo.c - Simple FreeRTOS example.
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
#include "driverlib/pin_map.h"
#include "driverlib/rom.h"
#include "driverlib/sysctl.h"
#include "driverlib/uart.h"
#include "utils/uartstdio.h"

#include "FreeRTOS.h"
#include "task.h"
#include "queue.h"
#include "semphr.h"

// TASKS
#include "comm_task.h"
#include "LCD_task.h"
#include "led_task.h"
#include "switch_task.h"
#include "ADC_task.h"
#include "mswitch_task.h"

#include "uart.h"

// --------------- TASK CONTROL ------------------

#define COMMTASK 1
#define LEDTASK 1
#define LCDTASK 1
#define SWITCHTASK 1
#define ADCTASK 1
#define MSWITCHTASK 1

// -----------------------------------------------

//*****************************************************************************
//
// The mutex that protects concurrent access of UART from multiple tasks.
//
//*****************************************************************************
xSemaphoreHandle g_pUARTSemaphore;

//*****************************************************************************
//
// The error routine that is called if the driver library encounters an error.
//
//*****************************************************************************
#ifdef DEBUG
void
__error__(char *pcFilename, uint32_t ui32Line)
{
}

#endif

//*****************************************************************************
//
// This hook is called by FreeRTOS when an stack overflow error is detected.
//
//*****************************************************************************
void
vApplicationStackOverflowHook(xTaskHandle *pxTask, char *pcTaskName)
{
    //
    // This function can not return, so loop forever.  Interrupts are disabled
    // on entry to this function, so no processor interrupts will interrupt
    // this loop.
    //
    while(1)
    {
    }
}

//*****************************************************************************
//
// Initialize FreeRTOS and start the initial set of tasks.
//
//*****************************************************************************
int
main(void)
{
    //
    // Set the clocking to run at 50 MHz from the PLL.
    //
    //ROM_SysCtlClockSet(SYSCTL_SYSDIV_4 | SYSCTL_USE_PLL | SYSCTL_XTAL_16MHZ |
    //                   SYSCTL_OSC_MAIN);

    //Clock set for LCD
    SysCtlClockSet(SYSCTL_SYSDIV_8|SYSCTL_USE_PLL|SYSCTL_XTAL_16MHZ|SYSCTL_OSC_MAIN);

    ConfigureUART();

    UARTprintf("\n\nWelcome to the EK-TM4C123GXL FreeRTOS Demo!\n");

    g_pUARTSemaphore = xSemaphoreCreateMutex();

    //
    // Create the LED task.
    //
    if(LEDTASK){
    if(LEDTaskInit() != 0)
      {

          while(1)
          {
            UARTprintf("\n\nLED INIT ERROR!\n");
          }
      }
    }

    //
    // Create the switch task.
    //
    if(SWITCHTASK){
      if(SwitchTaskInit() != 0){
        while(1)
        {
          UARTprintf("\n\nSWITCH INIT ERROR!\n");
        }
      }
    }

    //
    // Create the LCD task.
    //
    if(LCDTASK){
    if(LCDTaskInit() != 0){
        while(1)
        {
          UARTprintf("\n\nLCD INIT ERROR!\n");
        }
      }
    }

    //
    // Create the LCD task.
    //
    if(COMMTASK){
      if(CommTaskInit() != 0)
      {

          while(1)
          {
            UARTprintf("\n\nCOMM INIT ERROR!\n");
          }
      }
    }

    //
    // Create the ADC task.
    //
    if(ADCTASK){
      if(ADCTaskInit() != 0)
      {

          while(1)
          {
            UARTprintf("\n\nADC INIT ERROR!\n");
          }
      }
    }

    //
    // Create the ADC task.
    //
    if(MSWITCHTASK){
      if(MSWITCHTaskInit() != 0)
      {

          while(1)
          {
            UARTprintf("\n\nMSWITCH INIT ERROR!\n");
          }
      }
    }

    //
    // Start the scheduler.  This should not return.
    //
    vTaskStartScheduler();

    //
    // In case the scheduler returns for some reason, print an error and loop
    // forever.
    //
    UARTprintf("\n\nSCHEDULER RETURNED - ERROR!\n");
    while(1)
    {
    }
}
