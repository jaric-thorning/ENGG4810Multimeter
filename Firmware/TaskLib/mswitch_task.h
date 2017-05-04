/*
 *  Header file for MSWITCH_Task
 *
 *
 */



#ifndef __MSWITCH_TASK_H__
#define __MSWITCH_TASK_H__

extern struct mswitch_queue_message {
  char type;
  char mode;
  uint32_t ui32Value;
};

xQueueHandle g_pMSWITCHQueue;

//*****************************************************************************
//
// Prototypes for the MSWITCH task.
//
//*****************************************************************************
extern uint32_t MSWITCHTaskInit(void);




#endif // __MSWITCH_TASK_H__
