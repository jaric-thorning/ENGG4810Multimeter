/*
 *  Header file for LCD_Task
 *
 *
 */



#ifndef __LCD_TASK_H__
#define __LCD_TASK_H__

extern struct lcd_queue_message {
  char line1[16];
  char line2[16];
};

xQueueHandle g_pLCDQueue;




//*****************************************************************************
//
// Prototypes for the LED task.
//
//*****************************************************************************
extern uint32_t LCDTaskInit(void);




#endif // __LED_TASK_H__
