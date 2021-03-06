/*
 *  Header file for LCD_Task
 *
 *
 */

#ifndef __ADC_TASK_H__
#define __ADC_TASK_H__

extern struct adc_queue_message {
  int frequency;
  char mode;
}adc_queue_message;

xQueueHandle g_pADCQueue;
xSemaphoreHandle g_pSPISemaphore;

//*****************************************************************************
//
// Prototypes for the LED task.
//
//*****************************************************************************
extern uint32_t ADCTaskInit(void);




#endif // __LED_TASK_H__
