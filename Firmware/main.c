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

#define DATA_OUT GPIO_PIN_4


#define CONTROL_A GPIO_PIN_4
#define CONTROL_B GPIO_PIN_6
#define CONTROL_C GPIO_PIN_7
#define CONTROL_D GPIO_PIN_5
#define ALL_CONTROL_PINS CONTROL_A | CONTROL_B | CONTROL_C | CONTROL_D

#define CONTROL_13_V CONTROL_A
#define CONTROL_5_V CONTROL_B
#define CONTROL_1_V CONTROL_C
#define CONTROL_AMP CONTROL_D

#define CONTROL_10_mA CONTROL_1_V | CONTROL_AMP
#define CONTROL_200_mA CONTROL_1_V


//util includes
#include "utils/uart.h"
//#include "utils/display.h"

int mode = 1; //0 -> Voltage, 1 -> Current

int range = 13;
int range_current = 200;

void check_range(float value);
void check_current(float value);

int
main(void)
{
	//volatile uint32_t ui32Loop;



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



	//
	// Enable the GPIO port that is used for the on-board LED.
	//
	SysCtlPeripheralEnable(SYSCTL_PERIPH_GPIOF);
	SysCtlPeripheralEnable(SYSCTL_PERIPH_GPIOC);

	//defChar();
	initLCD();



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

	//
	// Check if the peripheral access is enabled.
	//
	while(!SysCtlPeripheralReady(SYSCTL_PERIPH_GPIOC))
	{
	}


	UARTprintf("Starting Screen\n");
	GPIOPinTypeGPIOOutput(GPIO_PORTC_BASE, ALL_CONTROL_PINS);

	//
	// Enable the GPIO pin for the LED (PF3).  Set the direction as output, and
	// enable the GPIO pin for digital function.
	//
	GPIOPinTypeGPIOOutput(GPIO_PORTF_BASE, RED_LED|BLUE_LED|GREEN_LED);

	//
	// Loop forever.
	//

	//int prev_value = 0;
	int integer = 0;
	int decimal = 0;

	int integer_current = 0;
	int decimal_current = 0;

	float value = 0;
	float value_current = 0;

	int delay = 1000000;

	//int range = 5;

	//GPIOPinWrite(GPIO_PORTC_BASE, ALL_CONTROL_PINS, 0x0); //set to 0


	//GPIOPinWrite(GPIO_PORTC_BASE, CONTROL_A, CONTROL_A);
	//GPIOPinWrite(GPIO_PORTC_BASE, CONTROL_A, 0x0);

	if(mode == 0){
		GPIOPinWrite(GPIO_PORTC_BASE, CONTROL_A, CONTROL_A);
		GPIOPinWrite(GPIO_PORTC_BASE, CONTROL_D, 0x0);
	}
	else{
		GPIOPinWrite(GPIO_PORTC_BASE, CONTROL_200_mA, CONTROL_200_mA);
		GPIOPinWrite(GPIO_PORTC_BASE, CONTROL_A, 0x0);
		GPIOPinWrite(GPIO_PORTC_BASE, CONTROL_B, 0x0);
	}

	while(1)
	{


		ADCProcessorTrigger(ADC0_BASE, 0);
		while(!ADCIntStatus(ADC0_BASE, 0, false))
		{
		}
		ADCSequenceDataGet(ADC0_BASE, 0, &ui32Value);

		value = ui32Value/4095.0 * 2 * range - range;
		value_current = ui32Value/4095.0 * 2 * range_current - range_current;
		integer = (int)value;
		decimal = ((int)(value*1000))%1000;
		if(decimal < 0){
			decimal *= -1;
		}

		integer_current = (int)value_current;
		decimal_current = ((int)(value_current*1000))%1000;
		if(decimal_current < 0){
			decimal_current *= -1;
		}

		if(mode == 0){


			check_range(value);
			UARTprintf("ADC: %d.%d\n", (int)(ui32Value/4095.0 * 3.3), ((int)(ui32Value/4095.0 * 3.3 *1000))%1000);
			UARTprintf("Voltage : %d.%d Range: %d\n", integer, decimal, range);
			display("voltage",range, integer, decimal);
		}
		else{
			check_current(value_current);
			UARTprintf("ADC: %d.%d\n", (int)(ui32Value/4095.0 * 3.3), ((int)(ui32Value/4095.0 * 3.3 *1000))%1000);
			UARTprintf("Current : %d.%d Range: %d\n", integer_current, decimal_current, range_current);
			display("current",range_current, integer_current, decimal_current);
		}



		//
		// Turn on the LED
		//
		GPIOPinWrite(GPIO_PORTF_BASE, RED_LED|BLUE_LED|GREEN_LED, GREEN_LED|BLUE_LED);

		SysCtlDelay(delay);



		GPIOPinWrite(GPIO_PORTF_BASE, RED_LED|BLUE_LED|GREEN_LED, 0x0);

		SysCtlDelay(delay);
	}
}

void check_range(float value){
	if( value < 0){
		value *= -1;
	}

	if( range == 13){
		if( value > 12){
			UARTprintf("Warning: Value out of range!\n");
			//Reset all
			GPIOPinWrite(GPIO_PORTC_BASE, CONTROL_1_V, 0x0);
			GPIOPinWrite(GPIO_PORTC_BASE, CONTROL_5_V, 0x0);
			GPIOPinWrite(GPIO_PORTC_BASE, CONTROL_13_V, 0x0);
			//Enable 13V
			GPIOPinWrite(GPIO_PORTC_BASE, CONTROL_13_V, CONTROL_13_V);
		}
		else if( value < 5){
			UARTprintf("Switching to 5V resolution\n");
			range = 5;
			//Switch Down

			GPIOPinWrite(GPIO_PORTC_BASE, CONTROL_13_V, 0x0);
			GPIOPinWrite(GPIO_PORTC_BASE, CONTROL_5_V, CONTROL_5_V);
		}
	}
	else if ( range == 5){
		if( value >= 5){
			UARTprintf("Switching to 12V resolution\n");
			range = 13;
			GPIOPinWrite(GPIO_PORTC_BASE, CONTROL_5_V, 0x0);
			GPIOPinWrite(GPIO_PORTC_BASE, CONTROL_13_V, CONTROL_13_V);

		}
		else if( value < 1){
			UARTprintf("Switching to 1V resolution\n");
			range = 1;
			GPIOPinWrite(GPIO_PORTC_BASE, CONTROL_5_V, 0x0);
			GPIOPinWrite(GPIO_PORTC_BASE, CONTROL_1_V, CONTROL_1_V);

		}

	}
	else if (range == 1){
		if( value >= 0.9){
			UARTprintf("Switching to 5V resolution\n");
			range = 5;
			GPIOPinWrite(GPIO_PORTC_BASE, CONTROL_1_V, 0x0);
			GPIOPinWrite(GPIO_PORTC_BASE, CONTROL_5_V, CONTROL_5_V);

		}
		else if( value < 1){
			//No worries
		}
	}
	else{
		//Shouldn't get here ever
		UARTprintf("Warning, range outside of normal values!\n");
	}
	return;
}

void check_current(float value){
	if( value < 0){
		value *= -1;
	}

	if( range_current == 200){
		if( value > 200){
			UARTprintf("Warning: Value out of range!\n");
			//Reset all
			GPIOPinWrite(GPIO_PORTC_BASE, CONTROL_10_mA, 0x0);
			GPIOPinWrite(GPIO_PORTC_BASE, CONTROL_200_mA, 0x0);
			//Enable 200mA
			GPIOPinWrite(GPIO_PORTC_BASE, CONTROL_200_mA, CONTROL_200_mA);
		}
		else if( value < 10){
			UARTprintf("Switching to 10mA resolution\n");
			range_current = 10;
			//Switch Down

			GPIOPinWrite(GPIO_PORTC_BASE, CONTROL_200_mA, 0x0);
			GPIOPinWrite(GPIO_PORTC_BASE, CONTROL_10_mA, CONTROL_10_mA);
		}
	}
	else if ( range_current == 10){
		if( value >= 9){
			UARTprintf("Switching to 200mA resolution\n");
			range_current = 200;
			GPIOPinWrite(GPIO_PORTC_BASE, CONTROL_10_mA, 0x0);
			GPIOPinWrite(GPIO_PORTC_BASE, CONTROL_200_mA, CONTROL_200_mA);

		}
		else if( value < 10){
			//No worries
		}
	}
	else{
		//Shouldn't get here ever
		UARTprintf("Warning, range outside of normal values!\n");
	}
	return;
}
