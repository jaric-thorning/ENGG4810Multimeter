/*
 *  Header file for BUZZER_Task
 *
 *
 */

#ifndef __BUZZER_TASK_H__
#define __BUZZER_TASK_H__

extern struct buzzer_queue_message {
  int frequency;
  int sound;
}buzzer_queue_message;

xQueueHandle g_pBuzzerQueue;

//*****************************************************************************
//
// Prototypes for the LED task.
//
//*****************************************************************************
extern uint32_t BuzzerTaskInit(void);




#endif // __LED_TASK_H__
