
#include <stdbool.h>
#include <stdint.h>
#include "stdio.h"
//#include "xxx.h"
#include "driverlib/rom.h"
#include "driverlib/gpio.h"
#include "driverlib/pin_map.h"
#include "utils/uartstdio.h"
#include "inc/hw_gpio.h"


#include "inc/hw_memmap.h"
#include "inc/hw_gpio.h"
#include "driverlib/sysctl.h"
#include "utils/uartstdio.h"

#define CS_PIN GPIO_PIN_6
#define DI_PIN GPIO_PIN_7
#define DO_PIN GPIO_PIN_6
#define SCLK_PIN GPIO_PIN_0
#define GPIO1_PIN GPIO_PIN_4

#define CS_PORT_BASE GPIO_PORTC_BASE
#define DI_PORT_BASE GPIO_PORTC_BASE
#define DO_PORT_BASE GPIO_PORTD_BASE
#define SCLK_PORT_BASE GPIO_PORTE_BASE
#define GPIO1_PORT_BASE GPIO_PORTF_BASE

#define CS_PERIPH_GPIO SYSCTL_PERIPH_GPIOC
#define DI_PERIPH_GPIO SYSCTL_PERIPH_GPIOC
#define DO_PERIPH_GPIO SYSCTL_PERIPH_GPIOD
#define SCLK_PERIPH_GPIO SYSCTL_PERIPH_GPIOE
#define GPIO1_PERIPH_GPIO SYSCTL_PERIPH_GPIOF

#define LOW 0
#define HIGH 1

#define DELAY_TIME 10

void pulse_sclk(void){
	GPIOPinWrite(SCLK_PORT_BASE, SCLK_PIN, 0);
	SysCtlDelay(DELAY_TIME);
	GPIOPinWrite(SCLK_PORT_BASE, SCLK_PIN, SCLK_PIN);
	SysCtlDelay(DELAY_TIME);
	GPIOPinWrite(SCLK_PORT_BASE, SCLK_PIN, 0);
	SysCtlDelay(DELAY_TIME);
	return;
}

void set_cs(int state){
	if(state){
		GPIOPinWrite(CS_PORT_BASE, CS_PIN, CS_PIN);
	}
	else{
		GPIOPinWrite(CS_PORT_BASE, CS_PIN, 0);
	}
	SysCtlDelay(DELAY_TIME);
}

void set_di(int bit){

	if(bit){
		GPIOPinWrite(DI_PORT_BASE, DI_PIN, DI_PIN);
	}
	else{
		GPIOPinWrite(DI_PORT_BASE, DI_PIN, 0);
	}
	SysCtlDelay(DELAY_TIME);
}

void send_command(uint8_t command){

	//UARTprintf("Sending Command\n\r");
	SysCtlDelay(DELAY_TIME);
	pulse_sclk();
	SysCtlDelay(DELAY_TIME);
	set_cs(LOW);

	//send command + address
	for(int i = 7; i >= 0; i--){
		set_di((command >> i) & 1);
		pulse_sclk();
	}

	SysCtlDelay(DELAY_TIME);
	set_cs(HIGH);
	SysCtlDelay(DELAY_TIME);
	pulse_sclk();

	return;
}

void write_byte(uint8_t c_add, uint8_t byte){

	UARTprintf("Writing Byte\n\r");
	//pulse_sclk();
	set_cs(LOW);

	//send command + address
	for(int i = 7; i >= 0; i--){
		set_di((c_add >> i) & 1);
		pulse_sclk();
	}

	//send byte
	for(int i = 7; i >= 0; i--){
		set_di((byte >> i) & 1);
		pulse_sclk();
	}

	set_cs(HIGH);
	pulse_sclk();

	return;
}

int get_do(void){
	int read_value = 0;
	read_value = GPIOPinRead(DO_PORT_BASE,DO_PIN);
	return read_value;
}

uint8_t read_byte(uint8_t address){
	uint8_t byte = 0;
	//UARTprintf("Reading Command\n\r");

	SysCtlDelay(DELAY_TIME);
	set_cs(LOW);
	SysCtlDelay(DELAY_TIME);
	//send command + address
	for(int i = 7; i >= 0; i--){
		set_di((address >> i) & 1);
		pulse_sclk();
	}
	SysCtlDelay(DELAY_TIME);
	//read byte
	for(int i = 7; i >= 0; i--){
		if(get_do()){
			byte |= 1 << i;
		}
		pulse_sclk();
	}
	SysCtlDelay(DELAY_TIME);
	set_cs(HIGH);
	SysCtlDelay(DELAY_TIME);
	pulse_sclk();
	SysCtlDelay(DELAY_TIME);

	return byte;
}

uint32_t read_data(void){
	uint32_t data = 0;
	uint8_t address = 0b11001001;
	set_cs(LOW);
	//send command + address
	for(int i = 7; i >= 0; i--){
		set_di((address >> i) & 1);
		pulse_sclk();
	}
	//read byte
	for(int i = 23; i >= 0; i--){
		if(get_do()){
			data |= 1 << i;
		}
		pulse_sclk();
	}
	set_cs(HIGH);
	pulse_sclk();
	return data;
}

void spi_adc_init(void){
	SysCtlPeripheralEnable(CS_PERIPH_GPIO);
	while(!SysCtlPeripheralReady(CS_PERIPH_GPIO))
	{
	}
	GPIOPinTypeGPIOOutput(CS_PORT_BASE, CS_PIN);

	SysCtlPeripheralEnable(DI_PERIPH_GPIO);
	while(!SysCtlPeripheralReady(DI_PERIPH_GPIO))
	{
	}
	GPIOPinTypeGPIOOutput(DI_PORT_BASE, DI_PIN);

	SysCtlPeripheralEnable(DO_PERIPH_GPIO);
	while(!SysCtlPeripheralReady(DO_PERIPH_GPIO))
	{
	}
	GPIOPinTypeGPIOInput(DO_PORT_BASE, DO_PIN);

	SysCtlPeripheralEnable(SCLK_PERIPH_GPIO);
	while(!SysCtlPeripheralReady(SCLK_PERIPH_GPIO))
	{
	}
	GPIOPinTypeGPIOOutput(SCLK_PORT_BASE, SCLK_PIN);

	set_cs(HIGH);

	//CAUSES LEFT BUTTON SPIKE
	//CHECK NOT LOCKED TO LEFT BUTTON
	/*SysCtlPeripheralEnable(GPIO1_PERIPH_GPIO);
	while(!SysCtlPeripheralReady(GPIO1_PERIPH_GPIO))
	{
	}
	GPIOPinTypeGPIOInput(GPIO1_PORT_BASE, GPIO1_PIN);*/

	UARTprintf("    SPI ADC initiated.\n\r");

}
