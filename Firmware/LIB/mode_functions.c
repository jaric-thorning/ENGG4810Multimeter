/*
 * Functions for Mode Changing
 */
//
#include <stdint.h>
#include <stdbool.h>
#include "inc/hw_memmap.h"
#include "driverlib/gpio.h"
#include "driverlib/rom.h"
#include "mode_functions.h"

uint8_t shift_reg = 0x00;

void set_shift_pin(int pin, int value){
   GPIOPinWrite(SHIFT_CLK_PIN_BASE, SHIFT_CLK_PIN, 0);
   shift_reg ^= (-value ^ shift_reg) & (1 << pin);
   //UARTprintf("Shift Register: ");
   for(int i = 0; i < 8; i ++){
     if((shift_reg >> (7 - i)) & 1){

       GPIOPinWrite(SHIFT_IN_PIN_BASE, SHIFT_IN_PIN, SHIFT_IN_PIN);
     } else{
       GPIOPinWrite(SHIFT_IN_PIN_BASE, SHIFT_IN_PIN, 0);
     }
     //UARTprintf("%d",(shift_reg >> i) & 1);
     GPIOPinWrite(SHIFT_CLK_PIN_BASE, SHIFT_CLK_PIN, SHIFT_CLK_PIN);
     SysCtlDelay(5000);
     GPIOPinWrite(SHIFT_CLK_PIN_BASE, SHIFT_CLK_PIN, 0);
   }
   //UARTprintf("\n\r");
 }

 void set_mode(int new_mode){
   //mode = new_mode;
   if(new_mode == DC_VOLTAGE){
     //write S1 to 12V mode (010)
     set_shift_pin(S1_C_PIN, 0);
     set_shift_pin(S1_B_PIN, 1);
     set_shift_pin(S1_A_PIN, 0);

     //write S2 to 200mA (11) and IHN to 1 (off)
     set_shift_pin(S2_B_PIN, 1);
     set_shift_pin(S2_A_PIN, 1);
     set_shift_pin(S2_I_PIN, 1);

     //write S3 to voltage mode (01)
     set_shift_pin(S3_B_PIN, 0);
     set_shift_pin(S3_A_PIN, 1);

   } else if (new_mode == AC_VOLTAGE){
     set_shift_pin(S1_C_PIN, 1);
     set_shift_pin(S1_B_PIN, 0);
     set_shift_pin(S1_A_PIN, 1);

     //write S2 to 200mA (11) and IHN to 1 (off)
     set_shift_pin(S2_B_PIN, 1);
     set_shift_pin(S2_A_PIN, 1);
     set_shift_pin(S2_I_PIN, 1);

     //write S3 to voltage mode (01)
     set_shift_pin(S3_B_PIN, 0);
     set_shift_pin(S3_A_PIN, 1);
   } else if ((new_mode == DC_CURRENT)|| (new_mode == AC_CURRENT)){
     //write S1 to 000
     set_shift_pin(S1_C_PIN, 0);
     set_shift_pin(S1_B_PIN, 0);
     set_shift_pin(S1_A_PIN, 0);

     //write S2 to 200mA (11) and IHN to 0 (on)
     set_shift_pin(S2_B_PIN, 1);
     set_shift_pin(S2_A_PIN, 1);
     set_shift_pin(S2_I_PIN, 0);

     //write S3 to current mode (00)
     set_shift_pin(S3_B_PIN, 0);
     set_shift_pin(S3_A_PIN, 0);

   } else if (new_mode == RESISTANCE){
     //write S1 to (000)
     set_shift_pin(S1_C_PIN, 0);
     set_shift_pin(S1_B_PIN, 0);
     set_shift_pin(S1_A_PIN, 0);

     //write S2 to 1MOhm (11) and IHN to 0 (on)
     set_shift_pin(S2_B_PIN, 0);
     set_shift_pin(S2_A_PIN, 0);
     set_shift_pin(S2_I_PIN, 0);

     //write S3 to resistance mode (10)
     set_shift_pin(S3_B_PIN, 1);
     set_shift_pin(S3_A_PIN, 0);
   } else if (new_mode == LOGIC){

     //write S1 to 12V mode (010)
     set_shift_pin(S1_C_PIN, 0);
     set_shift_pin(S1_B_PIN, 1);
     set_shift_pin(S1_A_PIN, 0);

     //write S2 to 200mA (11) and IHN to 1 (off)
     set_shift_pin(S2_B_PIN, 1);
     set_shift_pin(S2_A_PIN, 1);
     set_shift_pin(S2_I_PIN, 1);

     //write S3 to voltage mode (01)
     set_shift_pin(S3_B_PIN, 1);
     set_shift_pin(S3_A_PIN, 1);
   }
   return;
 }
