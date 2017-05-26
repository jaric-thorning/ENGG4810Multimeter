#ifndef __SWITCH_TASK_H__
#define __SWITCH_TASK_H__



extern struct switch_queue_message {
  char setting;
  int menu_on;
};

xQueueHandle g_pSWITCHQueue;


//*****************************************************************************
//
// Prototypes for the switch task.
//
//*****************************************************************************
extern uint32_t SwitchTaskInit(void);

#endif // __SWITCH_TASK_H__
