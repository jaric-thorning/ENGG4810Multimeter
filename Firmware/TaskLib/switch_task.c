#include <stdbool.h>
#include <stdint.h>
#include "inc/hw_memmap.h"
#include "inc/hw_types.h"
#include "inc/hw_gpio.h"
#include "driverlib/sysctl.h"
#include "driverlib/gpio.h"
#include "driverlib/rom.h"
#include "drivers/buttons.h"
#include "utils/uartstdio.h"
#include "led_task.h"
#include "priorities.h"
#include "FreeRTOS.h"
#include "task.h"
#include "queue.h"
#include "semphr.h"
#include "string.h"
#include "driverlib/systick.h"

#include "driverlib/debug.h"
#include "driverlib/gpio.h"
#include "driverlib/sysctl.h"
#include "driverlib/systick.h"
#include "grlib/grlib.h"
#include "utils/ustdlib.h"

#include "LCD_task.h"
///#include "xxx.h"
#include "mswitch_task.h"
#include "ADC_task.h"
#include "buzzer_task.h"
#include "lcd_task.h"
#include "inc/hw_hibernate.h"
#include "driverlib/hibernate.h"
#include "switch_task.h"

#define POWER_PIN GPIO_PIN_1
#define POWER_PORT GPIO_PORTF_BASE
#define POWER_PERIPH_GPIO SYSCTL_PERIPH_GPIOF

#define LED1_PIN GPIO_PIN_6
#define LED2_PIN GPIO_PIN_7
#define LED1_PORT GPIO_PORTA_BASE
#define LED2_PORT GPIO_PORTA_BASE
#define LED1_PERIPH_GPIO SYSCTL_PERIPH_GPIOA
#define LED2_PERIPH_GPIO SYSCTL_PERIPH_GPIOA

#define BUTTON3_PIN GPIO_PIN_3
#define BUTTON4_PIN GPIO_PIN_3
#define BUTTON5_PIN GPIO_PIN_2
#define BUTTON6_PIN GPIO_PIN_2

#define BUTTON3_PORT GPIO_PORTB_BASE
#define BUTTON4_PORT GPIO_PORTF_BASE
#define BUTTON5_PORT GPIO_PORTF_BASE
#define BUTTON6_PORT GPIO_PORTE_BASE

#define BUTTON3_PERIPH_GPIO SYSCTL_PERIPH_GPIOB
#define BUTTON4_PERIPH_GPIO SYSCTL_PERIPH_GPIOF
#define BUTTON5_PERIPH_GPIO SYSCTL_PERIPH_GPIOF
#define BUTTON6_PERIPH_GPIO SYSCTL_PERIPH_GPIOE

#define SWITCH_ITEM_SIZE           sizeof(struct switch_queue_message)
#define SWITCH_QUEUE_SIZE          5

#define SWITCHTASKSTACKSIZE        128         // Stack size in words

extern xQueueHandle g_pLEDQueue;
extern xSemaphoreHandle g_pUARTSemaphore;

static void
SwitchTask(void *pvParameters)
{
    portTickType ui16LastTime, currentTime;
    uint32_t ui32SwitchDelay = 25;
    uint8_t ui8CurButtonState, ui8PrevButtonState;
    uint8_t ui8Message;

    ui8CurButtonState = ui8PrevButtonState = 0;
    ui16LastTime = xTaskGetTickCount();

    struct mswitch_queue_message mswitch_message;
    struct adc_queue_message adc_message;
    struct buzzer_queue_message buzzer_message;
    struct lcd_queue_message lcd_message;
    struct switch_queue_message switch_message;

    uint32_t button3 = 0;
    uint32_t button4 = 0;
    uint32_t button5 = 0;
    uint32_t button6 = 0;

    int button3_time = 0;
    int button4_time = 0;
    int button5_time = 0;
    int button6_time = 0;

    int freq = 1;
    int brightness = 4;

    int menu_on = 0;

    int power_on = 1;
    int power_rec = 0;

    while(1)
    {
      if(xQueueReceive(g_pSWITCHQueue, &switch_message, 0) == pdPASS)
      {
        if(switch_message.setting == 'M'){
          if(switch_message.menu_on == 1){
            menu_on = 1;
          } else{
            menu_on = 0;
          }
        }
        if(switch_message.setled1){
          if(switch_message.led1){
            GPIOPinWrite(LED1_PORT,LED1_PIN,LED1_PIN);
          } else{
            GPIOPinWrite(LED1_PORT,LED1_PIN,0);
          }
        }
        if(switch_message.setled2){
          if(switch_message.led2){
            GPIOPinWrite(LED2_PORT,LED2_PIN,LED2_PIN);
          } else{
            GPIOPinWrite(LED2_PORT,LED2_PIN,0);
          }
        }
        if(switch_message.power){
          power_rec = 1;
          switch_message.power = 0;
        }
        //switch_message.setled1 = 0;
        //switch_message.setled2 = 0;
      }
        ui8CurButtonState = ButtonsPoll(0, 0);

        button3 = GPIOPinRead(BUTTON3_PORT,BUTTON3_PIN);
        button4 = GPIOPinRead(BUTTON4_PORT,BUTTON4_PIN);
        button5 = GPIOPinRead(BUTTON5_PORT,BUTTON5_PIN);
        button6 = GPIOPinRead(BUTTON6_PORT,BUTTON6_PIN);

        currentTime = xTaskGetTickCount();
        if((button3 == BUTTON3_PIN) && (button3_time + 200 < currentTime)){
          button3_time = currentTime;
            menu_on = 1;
            lcd_message.mode = 'M';
            lcd_message.button = 'B';
            if(xQueueSend(g_pLCDQueue, &lcd_message, portMAX_DELAY) !=
               pdPASS){
                 UARTprintf("FAILED TO SEND TO MSWITCH QUEUE\n\r");
               }
        }
        else if((button4 == BUTTON4_PIN) && (button4_time + 200 < currentTime)){
          //UARTprintf("BUTTON4 is HIGH!\n\r");
          button4_time = currentTime;

          if(menu_on){

            lcd_message.mode = 'M';
            lcd_message.button = 'N';
            lcd_message.setting = 0;
            if(xQueueSend(g_pLCDQueue, &lcd_message, portMAX_DELAY) !=
               pdPASS){
                 UARTprintf("FAILED TO SEND TO MSWITCH QUEUE\n\r");
               }

          } else{

            mswitch_message.value = 0; //doesn't matter
            mswitch_message.type = 'M'; //sending M for mode
            mswitch_message.mode = 'U'; //sending I to increment mode

            if(xQueueSend(g_pMSWITCHQueue, &mswitch_message, portMAX_DELAY) !=
               pdPASS){
                 UARTprintf("FAILED TO SEND TO MSWITCH QUEUE\n\r");
               }
        }
      }
        else if((button5 == BUTTON5_PIN) && (button5_time + 200 < currentTime)){

          //BUZZER TEST CODE
          buzzer_message.frequency = 1000;
          buzzer_message.sound = 1;
          if(xQueueSend(g_pBuzzerQueue, &buzzer_message, portMAX_DELAY) !=
             pdPASS){
               UARTprintf("FAILED TO SEND TO MSWITCH QUEUE\n\r");
             }


          if(menu_on){
            lcd_message.mode = 'M';
            lcd_message.button = 'S';
            lcd_message.setting = 0;
            if(xQueueSend(g_pLCDQueue, &lcd_message, portMAX_DELAY) !=
               pdPASS){
                 UARTprintf("FAILED TO SEND TO MSWITCH QUEUE\n\r");
               }

          } else{

                mswitch_message.value = 0; //doesn't matter
                mswitch_message.type = 'R'; //sending M for mode

                if(xQueueSend(g_pMSWITCHQueue, &mswitch_message, portMAX_DELAY) !=
                   pdPASS){
                     UARTprintf("FAILED TO SEND TO MSWITCH QUEUE\n\r");
                   }
                }



               //UARTprintf("Changing brightness\n\r");
               /*brightness = (brightness - 1);
               if(brightness < 0){
                 brightness = 4;
               }
               lcd_message.mode = 'X'; //don't care avoid value
               lcd_message.setting = 1;
               lcd_message.brightness = brightness;
               if(xQueueSend(g_pLCDQueue, &lcd_message, portMAX_DELAY) !=
                  pdPASS){
                    UARTprintf("FAILED TO SEND TO MSWITCH QUEUE\n\r");
                  }
          }*/

          /*freq = ((freq + 5) % 50) + 1;
          adc_message.frequency = freq;
          UARTprintf("Changing sampling frequency to: %d\n\r", freq * 100);
          if(xQueueSend(g_pADCQueue, &adc_message, portMAX_DELAY) !=
             pdPASS){
               UARTprintf("FAILED TO SEND TO MSWITCH QUEUE\n\r");
             }*/
          button5_time = currentTime;
        }
        else if((button6 != BUTTON6_PIN) && (button6_time + 200 < currentTime)){
            //UARTprintf("Power bit is: %d", power_on);
            if(power_on){
              GPIOPinWrite(POWER_PORT,GPIO_PIN_1,0);
              power_on = 0;
            } else{
              GPIOPinWrite(POWER_PORT,GPIO_PIN_1,GPIO_PIN_1);
              power_on = 1;
            }
           //HIBERNATION CODE -> DO NOT REMOVE
           /*if(HibernateIsActive()){
             UARTprintf("Hibernation Active.\n\r");
             uint32_t ui32Status = HibernateIntStatus(0);
             HibernateIntClear(ui32Status);
             if(ui32Status & HIBERNATE_INT_PIN_WAKE){
                 UARTprintf("BUTTON\n\r");
             } else if(ui32Status & HIBERNATE_INT_RTC_MATCH_0){
                 UARTprintf("TIMEOUT\n\r");
             } else
             {
                 UARTprintf("RESET\n\r");
             }
           }*/

           /*HibernateWakeSet(HIBERNATE_WAKE_PIN);
           int last_flash = 0;
           HibernateRequest();*/
           /*while(1){
             if(xTaskGetTickCount() > last_flash + 1500){
               GPIOPinWrite(LED1_PORT,LED1_PIN,LED1_PIN);
               if(xTaskGetTickCount() > last_flash + 1800){
                 GPIOPinWrite(LED1_PORT,LED1_PIN,0);
                 last_flash = xTaskGetTickCount();
               }
             }
           }*/

          button6_time = currentTime;
        }


        /*if(ui8CurButtonState != ui8PrevButtonState)
        {
            ui8PrevButtonState = ui8CurButtonState;


            if((ui8CurButtonState & ALL_BUTTONS) != 0)
            {
                if((ui8CurButtonState & ALL_BUTTONS) == LEFT_BUTTON)
                {
                    ui8Message = LEFT_BUTTON;

                    //
                    // Guard UART from concurrent access.
                    //
                    xSemaphoreTake(g_pUARTSemaphore, portMAX_DELAY);
                    UARTprintf("Left Button is pressed.\n");
                    xSemaphoreGive(g_pUARTSemaphore);

                    mswitch_message.ui32Value = 0; //doesn't matter
                    mswitch_message.type = 'M'; //sending M for mode
                    mswitch_message.mode = 'I'; //sending I to increment mode

                    if(xQueueSend(g_pMSWITCHQueue, &mswitch_message, portMAX_DELAY) !=
                       pdPASS){
                         UARTprintf("FAILED TO SEND TO MSWITCH QUEUE\n\r");
                       }

                }
                else if((ui8CurButtonState & ALL_BUTTONS) == RIGHT_BUTTON)
                {
                    ui8Message = RIGHT_BUTTON;

                    //
                    // Guard UART from concurrent access.
                    //
                    xSemaphoreTake(g_pUARTSemaphore, portMAX_DELAY);
                    UARTprintf("Right Button is pressed.\n");
                    xSemaphoreGive(g_pUARTSemaphore);
                }

                if(xQueueSend(g_pLEDQueue, &ui8Message, portMAX_DELAY) !=
                   pdPASS)
                {
                    UARTprintf("\nQueue full. This should never happen.\n");
                    while(1)
                    {
                    }
                }
            }
        }*/

        vTaskDelayUntil(&ui16LastTime, ui32SwitchDelay / portTICK_RATE_MS);
    }
}

uint32_t
SwitchTaskInit(void)
{
    g_pSWITCHQueue = xQueueCreate(SWITCH_QUEUE_SIZE, SWITCH_ITEM_SIZE);

    //HWREG(GPIO_PORTF_BASE + GPIO_O_LOCK) = GPIO_LOCK_KEY;
    //HWREG(GPIO_PORTF_BASE + GPIO_O_CR) = 0xFF;

    ButtonsInit();

    // HIBERNATE SETUP

    //
    // Set up systick to generate interrupts at 100 Hz.
    //
    /*SysTickPeriodSet(SysCtlClockGet() / 100);
    SysTickIntEnable();
    SysTickEnable();*/

    //
    // Enable the Hibernation module.
    //

    SysCtlPeripheralEnable(POWER_PERIPH_GPIO);
    while(!SysCtlPeripheralReady(POWER_PERIPH_GPIO))
	  {
	  }
    GPIOPinTypeGPIOOutput(POWER_PORT, GPIO_PIN_1);

    GPIOPinWrite(POWER_PORT,GPIO_PIN_1,GPIO_PIN_1);


    SysCtlPeripheralEnable(LED1_PERIPH_GPIO);
    while(!SysCtlPeripheralReady(LED1_PERIPH_GPIO))
	  {
	  }
    GPIOPinTypeGPIOOutput(LED1_PORT, GPIO_PIN_6);

    GPIOPinWrite(LED1_PORT,LED1_PIN,LED1_PIN);

    SysCtlPeripheralEnable(LED2_PERIPH_GPIO);
    while(!SysCtlPeripheralReady(LED2_PERIPH_GPIO))
	  {
	  }
    GPIOPinTypeGPIOOutput(LED2_PORT, GPIO_PIN_7);

    SysCtlPeripheralEnable(SYSCTL_PERIPH_HIBERNATE);

    //External Button Init
    SysCtlPeripheralEnable(BUTTON3_PERIPH_GPIO);
    while(!SysCtlPeripheralReady(BUTTON3_PERIPH_GPIO))
	  {
	  }
    GPIOPinTypeGPIOInput(BUTTON3_PORT, BUTTON3_PIN);

    SysCtlPeripheralEnable(BUTTON4_PERIPH_GPIO);
    while(!SysCtlPeripheralReady(BUTTON4_PERIPH_GPIO))
	  {
	  }
    GPIOPinTypeGPIOInput(BUTTON4_PORT, BUTTON4_PIN);

    SysCtlPeripheralEnable(BUTTON5_PERIPH_GPIO);
    while(!SysCtlPeripheralReady(BUTTON5_PERIPH_GPIO))
	  {
	  }
    GPIOPinTypeGPIOInput(BUTTON5_PORT, BUTTON5_PIN);

    SysCtlPeripheralEnable(BUTTON6_PERIPH_GPIO);
    while(!SysCtlPeripheralReady(BUTTON6_PERIPH_GPIO))
	  {
	  }
    GPIOPinTypeGPIOInput(BUTTON6_PORT, BUTTON6_PIN);

    if(xTaskCreate(SwitchTask, (signed portCHAR *)"Switch",
                   SWITCHTASKSTACKSIZE, NULL, tskIDLE_PRIORITY +
                   PRIORITY_SWITCH_TASK, NULL) != pdTRUE)
    {
        return(1);
    }
    UARTprintf("    Switch initiated.\n\r");
    //
    // Success.
    //
    return(0);
}
