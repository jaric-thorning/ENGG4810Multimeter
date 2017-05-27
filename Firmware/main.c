
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
#include "sd_task.h"
#include "buzzer_task.h"

#include "uart.h"


#define POWER_PIN GPIO_PIN_1
#define POWER_PORT GPIO_PORTF_BASE
#define POWER_GPIO SYSCTL_PERIPH_GPIOF


// --------------- TASK CONTROL ------------------

#define LEDTASK         1
#define SWITCHTASK      1

#define LCDTASK         1 //works
#define COMMTASK        1
#define ADCTASK         1 //works
#define MSWITCHTASK     1 //works
#define SDTASK          1
#define BUZZERTASK      1

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
    ROM_FPULazyStackingEnable();

    //Clock set for LCD
    SysCtlClockSet(SYSCTL_SYSDIV_8|SYSCTL_USE_PLL|SYSCTL_XTAL_16MHZ|SYSCTL_OSC_MAIN);

    ConfigureUART();
    UARTprintf("===============================================================\n\r");
    UARTprintf("     ____________\n\r");
    UARTprintf("     |   \\XX/   |\n\r");
    UARTprintf("     | T. \\/ .T |      University of Queensland\n\r");
    UARTprintf("     | XX:  :XX |          Faculty of EAIT\n\r");
    UARTprintf("     T L' /\\ 'J T\n\r");
    UARTprintf("      \\  /XX\\  /       ENGG4810 Team Project 2017\n\r");
    UARTprintf("   @\\_ '______' _/@       Jaric Thorning\n\r");
    UARTprintf("   \\_X\\_ ____ _/X_/       Yin Wu\n\r");
    UARTprintf("     \\=/\\----/\\=/         Daya Kern\n\r");
    UARTprintf("\n\r===============================================================\n\r");
    UARTprintf("\n\r---------------------------------------------------------------\n\r");
    UARTprintf("Initialising Components...\n\r");

    g_pUARTSemaphore = xSemaphoreCreateMutex();

    //ENABLE POWER

    //External Button Init
    /*SysCtlPeripheralEnable(POWER_GPIO);
    while(!SysCtlPeripheralReady(POWER_GPIO))
	  {
	  }
    GPIOPinTypeGPIOOutput(POWER_PORT, POWER_PIN);

    GPIOPinWrite(POWER_PORT, POWER_PIN, POWER_PIN);


    UARTprintf("Enabling Power...\n\r");*/

    // Create the LED task.
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

    // Create the Buzzer task.
    if(BUZZERTASK){
      if(BuzzerTaskInit() != 0){
          while(1){
            UARTprintf("\n\nBUZZER INIT ERROR!\n");
          }
      }
    }

    // Create the LCD task.
    if(LCDTASK){
    if(LCDTaskInit() != 0){
        while(1)
        {
          UARTprintf("\n\nLCD INIT ERROR!\n");
        }
      }
    }

    // Create the COMM task.
    if(COMMTASK){
      if(CommTaskInit() != 0){
          while(1){
            UARTprintf("\n\nCOMM INIT ERROR!\n");
          }
      }
    }

    // Create the ADC task.
    if(ADCTASK){
      if(ADCTaskInit() != 0){
          while(1){
            UARTprintf("\n\nADC INIT ERROR!\n");
          }
      }
    }

    // Create the MSWITCH task.
    if(MSWITCHTASK){
      if(MSWITCHTaskInit() != 0)
      {
          while(1){
            UARTprintf("\n\nMSWITCH INIT ERROR!\n");
          }
      }
    }

    // Create the SD task.
    if(SDTASK){
      if(SDTaskInit() != 0){
          while(1){
            UARTprintf("\n\nSD INIT ERROR!\n");
          }
      }
    }

    UARTprintf("Starting Scheduler...\n\r");
    UARTprintf("---------------------------------------------------------------\n\r");


    // Start the scheduler.  This should not return.
    vTaskStartScheduler();

    // In case the scheduler returns for some reason, print an error and loop
    // forever.
    UARTprintf("\n\nSCHEDULER RETURNED - ERROR!\n");
    while(1)
    {
    }
}
