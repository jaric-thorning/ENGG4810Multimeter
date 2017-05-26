/*
 *  Header file for menu.c
 *
 *
 */

#ifndef __MENU_H__
#define __MENU_H__

struct Menu {
  char* title;
  char* new_title;
  char* item;
  char* new_item;

  int selection;
  int * selecteditem;

	int active;
	portTickType last_title_scroll_time;

  int scroll_title;
  int title_count;

  portTickType last_item_scroll_time;

  int scroll_item;
  int item_count;

  char ** items;

};

typedef struct Menu *Menu;

void init_menu(struct Menu *menu);
int menu_active(struct Menu *menu);
void launch_menu(struct Menu *menu);
void format_menu(struct Menu *menu, char** line1, char** line2);
int update_title(struct Menu *menu);
void update_item(struct Menu *menu);
char * get_text(int selection, int item);
#endif // __LED_TASK_H__
