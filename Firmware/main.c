//*****************************************************************************
//
//*****************************************************************************

#include <stdint.h>
#include <stdbool.h>
#include "inc/hw_types.h"
#include "inc/hw_memmap.h"
#include "driverlib/gpio.h"
#include "driverlib/sysctl.h"
#include "driverlib/uart.h"
#include "driverlib/adc.h"

#include "math.h"

#include "LIB/display_functions.h"
#include "driverlib/rom.h"
#include "utils/uartstdio.h"
#include "driverlib/pin_map.h"

//#include "includes/TM4C123GXL_pin_map.h"

#include "LIB/display.h"

#include "driverlib/fpu.h"

#include "grlib/grlib.h"

#include "utils/uartstdio.h"

//#include "LIB/display.h"


#define RED_LED   GPIO_PIN_1
#define BLUE_LED  GPIO_PIN_2
#define GREEN_LED GPIO_PIN_3

//util includes
#include "utils/uart.h"
//#include "utils/display.h"

int
main(void)
{
	volatile uint32_t ui32Loop;



	// Enable lazy stacking for interrupt handlers.  This allows floating-point
	// instructions to be used within interrupt handlers, but at the expense of
	// extra stack usage.
	//
	ROM_FPULazyStackingEnable();

	//_______________________________________________________________________
	uint32_t ui32Value;
	//
	// Enable the ADC0 module.
	//
	SysCtlPeripheralEnable(SYSCTL_PERIPH_ADC0);
	//
	// Wait for the ADC0 module to be ready.
	//
	while(!SysCtlPeripheralReady(SYSCTL_PERIPH_ADC0))
	{
	}
	//
	// Enable the first sample sequencer to capture the value of channel 0 when
	// the processor trigger occurs.
	//
	ADCSequenceConfigure(ADC0_BASE, 0, ADC_TRIGGER_PROCESSOR, 0);
	ADCSequenceStepConfigure(ADC0_BASE, 0, 0,
							 ADC_CTL_IE | ADC_CTL_END | ADC_CTL_CH0);
	ADCSequenceEnable(ADC0_BASE, 0);
	//
	// Trigger the sample sequence.
	//
	ADCProcessorTrigger(ADC0_BASE, 0);
	//
	// Wait until the sample sequence has completed.
	//
	while(!ADCIntStatus(ADC0_BASE, 0, false))
	{
	}
	//
	// Read the value from the ADC.
	//
	ADCSequenceDataGet(ADC0_BASE, 0, &ui32Value);

	//_____________________________________________



	//Clock set for LCD
	SysCtlClockSet(SYSCTL_SYSDIV_8|SYSCTL_USE_PLL|SYSCTL_XTAL_16MHZ|SYSCTL_OSC_MAIN);

	//defChar();
	initLCD();

	//
	// Enable the GPIO port that is used for the on-board LED.
	//
	SysCtlPeripheralEnable(SYSCTL_PERIPH_GPIOF);


	//
	// Initialize the UART.
	//
	ConfigureUART();

	UARTprintf("Hello, world!\n");

	//
	// Check if the peripheral access is enabled.
	//
	while(!SysCtlPeripheralReady(SYSCTL_PERIPH_GPIOF))
	{
	}

	UARTprintf("Starting Screen\n");
	//
	// Initialize Screen
	//

	//initLCD();
	//setBlockCursorLCD();
	//printLCD("Hello World");



	/*set_data_out(0, 1);
    set_data_out(1, 1);
    set_data_out(2, 1);
    set_data_out(3, 1);
    set_data_out(4, 1);
    set_data_out(5, 1);
    set_data_out(6, 1);
    set_data_out(7, 1);*/

	//
	// Enable the GPIO pin for the LED (PF3).  Set the direction as output, and
	// enable the GPIO pin for digital function.
	//
	GPIOPinTypeGPIOOutput(GPIO_PORTF_BASE, RED_LED|BLUE_LED|GREEN_LED);

	//Set PF4 High
	//GPIOPinWrite(GPIO_PORTF_BASE, GPIO_PIN_4, GPIO_PIN_4);

	UARTprintf("Got here\n");
	//
	// Loop forever.
	//

	int prev_value = 0;
	int integer = 0;
	int decimal = 0;


	int i = 0;

	//int range = 15;
	//int range_multiplyer = range * 1000;
	//int integer2;
	while(1)
	{
		//Output to LCD Display
		/*printLCD("Tiva");
        setCursorPositionLCD(1,0);
        printLCD("LCD Display");
        SysCtlDelay(100000000);
        clearLCD();
        printLCD("16 characters");
        setCursorPositionLCD(1,0);
        printLCD("2 lines");
        SysCtlDelay(100000000);
        clearLCD();*/
		//
		// Trigger the sample sequence.
		//
		ADCProcessorTrigger(ADC0_BASE, 0);

		//
		// Wait until the sample sequence has completed.
		//
		while(!ADCIntStatus(ADC0_BASE, 0, false))
		{
		}
		//
		// Read the value from the ADC.
		//


		ADCSequenceDataGet(ADC0_BASE, 0, &ui32Value);

		UARTprintf("ui32Value : %d\n", ui32Value);

		integer = (((((ui32Value * 1000)/4098) * 3300)/1000000));
		decimal = ((((ui32Value * 1000)/4098) * 3300)%1000000)/100;

		//integer = (ui32Value/4095.0 * 30) - 15;
		//decimal = (int)(((ui32Value/4095.0 * 30 - 15) * 1000))%1000;
		/*decimal = 0;
        if(decimal < 0){
            decimal = -1 * decimal;
        }*/

		//integer2 = (integer * 30)/3.3 - 15;

		//UARTprintf(">>>>>>>Read voltage: %d.%d\n", integer);
		UARTprintf("Real voltage is: %d.%d\n", integer, decimal);

		//integer = (((((ui32Value * 1000)/4098) * 2 * range  - range_multiplyer)/1000000)/3.3);
		//decimal = ((((ui32Value * 1000)/4098) * 2 * range  - range_multiplyer)%1000000)/100;

		display("voltage",5, integer, decimal);

		if(decimal != prev_value){
			//UARTprintf("ADC Voltage: %d.%d\n",integer, decimal);

			prev_value = decimal;
			//UARTprintf("prev: %d, decimal: %d\n", prev_value, decimal);
		}

		//UARTprintf("double: %d\n",temp);
		//UARTprintf("ADC: %d\n",((ui32Value*10000) / 4098.0) * 3.3);

		//clearLCD();


		//sendByte(0x00, TRUE);

		//
		// Turn on the LED
		//
		GPIOPinWrite(GPIO_PORTF_BASE, RED_LED|BLUE_LED|GREEN_LED, GREEN_LED|BLUE_LED);
		//UARTprintf("LED on\n");
		//
		// Delay for a bit
		//
		SysCtlDelay(400000);

		//
		// Turn off the LED.
		//
		GPIOPinWrite(GPIO_PORTF_BASE, RED_LED|BLUE_LED|GREEN_LED, 0x0);
		//UARTprintf("LED off\n");
		//
		// Delay for a bit
		//
		SysCtlDelay(400000);
	}
}
