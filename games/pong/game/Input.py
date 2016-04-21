import pygame

class Input:
    val = 0
    playerId = 0

    def __init__(self, playerId):
        self.playerId = playerId

    def update(self):
        pass

    def onevent(self, event):
        pass

class KeyboardInput(Input):
    key_up = None
    key_down = None
    pressed_up = False
    pressed_down = False

    def __init__(self, playerId, keys):
        self.key_up, self.key_down = keys
        super().__init__(playerId)

    def update(self):
        if self.pressed_up and not self.pressed_down:
            self.val = 1
        elif self.pressed_down and not self.pressed_up:
            self.val = -1
        else:
            self.val = 0

    def onevent(self, event):
        if event.type == pygame.KEYDOWN:
            if event.key == self.key_up:
                self.pressed_up = True
            elif event.key == self.key_down:
                self.pressed_down = True
        elif event.type == pygame.KEYUP:
            if event.key == self.key_up:
                self.pressed_up = False
            elif event.key == self.key_down:
                self.pressed_down = False

class ControllerInput(Input):
    stick = None
    stick_id = None

    def __init__(self, playerId, stick, axis):
        self.stick = stick
        self.stick_id = stick.get_id()
        self.axis = axis
        super().__init__(playerId)

    def onevent(self, event):
        if (event.type == pygame.JOYAXISMOTION and
                event.joy == self.stick_id and
                event.axis == self.axis):
            if abs(event.value) > 0.15:
                self.val = event.value
            else:
                self.val = 0
