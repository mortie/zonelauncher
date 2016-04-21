#!/usr/bin/env python3

from Game import Game
from Input import ControllerInput
from Input import KeyboardInput
import pygame
import sys
import time

class Buttons:
    A = 0
    B = 1
    X = 2
    Y = 3
    START = 7
    K_ADD = 97
    K_ENTER = 13

# Audio
pygame.mixer.pre_init(44100, -16, 2, 2048)
pygame.mixer.init()

# Pygame
pygame.init()
screen = pygame.display.set_mode((1920, 1080))
pygame.display.toggle_fullscreen()

# Font
pygame.font.init()
font = pygame.font.SysFont(None, 48)

# Inputs
inputs = [
    KeyboardInput(1, (pygame.K_w, pygame.K_s)),
    KeyboardInput(2, (pygame.K_UP, pygame.K_DOWN))
]

def play():
    game = None
    game = Game(screen, inputs)
    return game.run()

while True:
    loser = play()

    screen.fill((0, 0, 0))
    label = font.render("Player "+str(loser)+" lost!", 1, (255, 255, 255))
    screen.blit(label, (100, 200))
    pygame.display.update()
    while True:
        evt = pygame.event.wait()
        if evt.type == pygame.JOYBUTTONDOWN and evt.button == Buttons.START:
            break
        elif evt.type == pygame.KEYDOWN and evt.key == Buttons.K_ENTER:
            break
        elif evt.type == pygame.QUIT:
            pygame.quit()
            sys.exit()
