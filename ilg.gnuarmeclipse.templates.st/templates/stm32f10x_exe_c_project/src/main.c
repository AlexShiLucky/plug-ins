/*
 ============================================================================
 Name        : $(baseName).c
 Author      : $(author)
 Version     :
 Copyright   : $(copyright)
 Description : Hello World in C
 ============================================================================
 */

#include <stdio.h>

#include "stm32f10x.h"

/*

 Print a greeting message on standard output and start blink a led.

 On embedded platforms printing the message might require
 semi-hosting or similar.

 For example, for toolchains derived from GNU Tools for Embedded,
 to enable semi-hosting, the following should be added to the linker:

 --specs=rdimon.specs -Wl,--start-group -lgcc -lc -lc -lm -lrdimon -Wl,--end-group

 */

/* Olimex STM32-H103 LED definitions */
#define BLINK_PORT      GPIOC
#define BLINK_PIN       12
#define BLINK_RCC_BIT   RCC_APB2Periph_GPIOC

#define BLINK_LOOPS     1000000

int
main(void)
{
  printf("$(messagearm)" "\n");

  GPIO_InitTypeDef GPIO_InitStructure;

  uint32_t i;

  /*!< At this stage the microcontroller clock setting is already configured,
   this is done through SystemInit() function which is called from startup
   file (startup_stm32f10x_xx.s) before to branch to application main.
   To reconfigure the default setting of SystemInit() function, refer to
   system_stm32f10x.c file
   */

  /* GPIO Periph clock enable */
  RCC_APB2PeriphClockCmd(BLINK_RCC_BIT, ENABLE);

  /* Configure pin in output push/pull mode */
  GPIO_InitStructure.GPIO_Pin = BLINK_PIN;
  GPIO_InitStructure.GPIO_Speed = GPIO_Speed_50MHz;
  GPIO_InitStructure.GPIO_Mode = GPIO_Mode_Out_PP;
  GPIO_Init(BLINK_PORT, &GPIO_InitStructure);

  while (1)
    {
      /* Set pin */
      GPIO_SetBits(BLINK_PORT, (1 << BLINK_PIN));

      i = 2 * BLINK_LOOPS;
      while (--i)
        ;

      /* Reset pin */
      GPIO_ResetBits(BLINK_PORT, (1 << BLINK_PIN));

      i = BLINK_LOOPS;
      while (--i)
        ;
    }

  return 0;
}
