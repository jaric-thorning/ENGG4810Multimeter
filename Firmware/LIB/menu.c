#include <stdbool.h>
#include <stdint.h>
#include <string.h>
#include "inc/hw_memmap.h"
#include "inc/hw_types.h"
#include "driverlib/gpio.h"
#include "driverlib/rom.h"
#include "drivers/rgb.h"
#include "drivers/buttons.h"
#include "utils/uartstdio.h"
#include "led_task.h"
#include "priorities.h"
#include "FreeRTOS.h"
#include "task.h"
#include "queue.h"
#include "semphr.h"

#include "bget.h"


#include "LCD_task.h"

#include "stdlib.h"
//LCD INCLUDES

#include "display_functions.h"
#include "driverlib/rom.h"
#include "utils/uartstdio.h"
#include "driverlib/pin_map.h"
#include "driverlib/sysctl.h"
#include "driverlib/systick.h"

#include "display.h"

#include "menu.h"

#define TITLESIZE 16
#define ITEMSIZE 16

#define NUM_MENUS 3
#define NUM_ITEMS 5

#define DEFAULT_TITLE "Main Menu"
#define DEFAULT_ITEM "Frequency"

void init_menu(Menu menu){

	menu->title = bgetz(sizeof(char) * TITLESIZE);
	menu->new_title = bgetz(sizeof(char) * TITLESIZE);

	menu->item  = bgetz(sizeof(char) * ITEMSIZE);
	menu->new_item  = bgetz(sizeof(char) * ITEMSIZE);

	menu->active = 0;
	menu->selection = 0;
	menu->selecteditem = bgetz(sizeof(int) * NUM_MENUS);
	menu->last_title_scroll_time = 0;
	menu->scroll_title = 0;
	menu->title_count = 0;

	menu->last_item_scroll_time = 0;
	menu->scroll_item = 0;
	menu->item_count = 0;

	menu->items = bgetz(8 * 16 * NUM_MENUS * NUM_ITEMS);

	for(int i = 0; i < NUM_MENUS; i++){
		UARTprintf("i: %d\n\r", i);
		menu->items[i] = bgetz(8 * 16 * NUM_ITEMS);
		for(int j = 0; j < NUM_ITEMS; j++){
			menu->items[i][j] = bgetz(8 * 16);
			//menu->items[i][j] = "                ";
		}
	}
}

int menu_active(Menu menu){
	return menu->active;
}
void launch_menu(Menu menu){
	menu->active = 1;
}

void format_menu(Menu menu, char** line1, char** line2){
	*line1 = menu->title;
	*line2 = menu->item;
	return;
}

void update_selection(Menu menu, int incrementation){
	menu->selection += incrementation;

	if(menu->selection == 0){
		//main menu
	} else if(menu->selection == 1){
		//frequency
	}
}

int update_title(Menu menu){
	static char update[] = "                ";
	if(menu->scroll_title){
		//UARTprintf("Scrolling Menu...\n\r");
		if(xTaskGetTickCount() > menu->last_title_scroll_time + 20){
				for(int i = 0; i < 16 - 1; i++){
					//UARTprintf("%c -> %c\n\r", update[i], menu->title[i + 1]);
					update[i] = menu->title[i + 1];
				}
				update[15] = menu->new_title[menu->title_count++];
				//UARTprintf("%c -> %c\n\r", menu->title[15], menu->title[menu->title_count-1]);
				//UARTprintf("UPDATE: %s\n\r", update);
				menu->title = (char*)&update;
				if(menu->title_count >= 16){
					menu->scroll_title = 0;
					menu->title_count = 0;
					return 1;
				}
				//UARTprintf("Current Title: %s\n\r", current_title);
		menu->last_title_scroll_time = xTaskGetTickCount();
		}
		return 0;
	}
	else{
		menu->title = menu->new_title;
		return 1;
	}
}

void update_item(Menu menu){
	static char update[] = "                ";
	if(menu->scroll_item){

		if(xTaskGetTickCount() > menu->last_item_scroll_time + 20){
				for(int i = 0; i < 16 - 1; i++){
					update[i] = menu->item[i + 1];
				}

				update[15] = menu->new_item[menu->item_count++];
				//UARTprintf("%c -> %c\n\r", menu->item[15], menu->item[menu->item_count-1]);
				//UARTprintf("UPDATE: %s\n\r", update);

				menu->item = (char*)&update;

				if(menu->item_count >= 16){

					menu->scroll_item = 0;
					menu->item_count = 0;
				}

				//UARTprintf("Current item: %s\n\r", current_item);
		menu->last_item_scroll_time = xTaskGetTickCount();
		}
	}
	else{
		menu->item = menu->new_item;
	}
}

char * get_text(int selection, int item){
	char* temp = bgetz(16 * 8);
	if(selection == 0){
		if(item == 0){
			temp = "Frequency       ";
		} else if (item == 1){
			temp = "Brightness      ";
		}
	} else if (selection == 1){
		if(item == 0){
			temp = "10              ";
		} else if(item == 1){
			temp = "100             ";
		}
	} else if (selection == 2){
		if(item == 0){
			temp = "5               ";
		} else if(item == 1){
			temp = "10              ";
		}
	}
	return temp;
}
